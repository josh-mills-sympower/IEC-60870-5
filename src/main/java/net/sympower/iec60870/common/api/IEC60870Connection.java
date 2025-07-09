/*
 * Original work: Copyright 2014-20 Fraunhofer ISE (OpenMUC j60870)
 *
 * Modified work: Copyright 2025 Sympower
 *
 * This file is part of the enhanced IEC 60870 library.
 * Original project: https://github.com/openmuc/j60870
 * Enhanced version: https://github.com/josh-mills-sympower/IEC-60870-5
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */
package net.sympower.iec60870.common.api;

import net.sympower.iec60870.common.ASdu;
import net.sympower.iec60870.common.CauseOfTransmission;
import net.sympower.iec60870.common.IEC60870Settings;
import net.sympower.iec60870.common.IEC60870Protocol;
import net.sympower.iec60870.common.elements.IeAckFileOrSectionQualifier;
import net.sympower.iec60870.common.elements.IeBinaryStateInformation;
import net.sympower.iec60870.common.elements.IeChecksum;
import net.sympower.iec60870.common.elements.IeDoubleCommand;
import net.sympower.iec60870.common.elements.IeFileReadyQualifier;
import net.sympower.iec60870.common.elements.IeFileSegment;
import net.sympower.iec60870.common.elements.IeLengthOfFileOrSection;
import net.sympower.iec60870.common.elements.IeNameOfFile;
import net.sympower.iec60870.common.elements.IeNameOfSection;
import net.sympower.iec60870.common.elements.IeNormalizedValue;
import net.sympower.iec60870.common.elements.IeQualifierOfCounterInterrogation;
import net.sympower.iec60870.common.elements.IeQualifierOfInterrogation;
import net.sympower.iec60870.common.elements.IeQualifierOfParameterOfMeasuredValues;
import net.sympower.iec60870.common.elements.IeQualifierOfSetPointCommand;
import net.sympower.iec60870.common.elements.IeRegulatingStepCommand;
import net.sympower.iec60870.common.elements.IeScaledValue;
import net.sympower.iec60870.common.elements.IeSectionReadyQualifier;
import net.sympower.iec60870.common.elements.IeShortFloat;
import net.sympower.iec60870.common.elements.IeSingleCommand;
import net.sympower.iec60870.common.elements.IeTestSequenceCounter;
import net.sympower.iec60870.common.elements.IeTime56;
import net.sympower.iec60870.common.elements.InformationElement;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class IEC60870Connection {

    protected final DataInputStream inputStream;
    protected final DataOutputStream outputStream;
    protected final IEC60870Settings settings;
    protected final ExecutorService executor;
    protected final AtomicBoolean closed = new AtomicBoolean(false);
    protected final AtomicBoolean dataTransferStarted = new AtomicBoolean(false);
    
    protected volatile IEC60870EventListener eventListener;
    protected int originatorAddress = 0;

    public IEC60870Connection(DataInputStream inputStream, DataOutputStream outputStream, IEC60870Settings settings) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        this.settings = settings;
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
    }

    
    public abstract void startDataTransfer(IEC60870EventListener listener) throws IOException;

    
    public abstract void stopDataTransfer() throws IOException;

    
    public void close() {
        if (closed.getAndSet(true)) {
            return;
        }
        
        dataTransferStarted.set(false);
        executor.shutdown();
        
        try {
            performClose();
        } catch (IOException e) {
            // Log but don't throw from close()
        }
        
        if (eventListener != null) {
            eventListener.onConnectionLost(null);
        }
    }

    
    public boolean isClosed() {
        return closed.get();
    }

    
    public abstract void send(ASdu asdu) throws IOException;

    
    public void sendConfirmation(ASdu asdu) throws IOException {
        send(IEC60870Protocol.createConfirmation(asdu, originatorAddress));
    }

    protected abstract void performClose() throws IOException;

    protected abstract void readerTask();


    public void interrogation(int commonAddress, CauseOfTransmission cot, IeQualifierOfInterrogation qualifier)
            throws IOException {
        send(IEC60870Protocol.interrogation(originatorAddress, commonAddress, cot, qualifier));
    }

    
    public void counterInterrogation(int commonAddress, CauseOfTransmission cot,
                                     IeQualifierOfCounterInterrogation qualifier) throws IOException {
        send(IEC60870Protocol.counterInterrogation(originatorAddress, commonAddress, cot, qualifier));
    }

    
    public void read(int commonAddress, int informationObjectAddress) throws IOException {
        send(IEC60870Protocol.read(originatorAddress, commonAddress, informationObjectAddress));
    }

    
    public void synchronizeClocks(int commonAddress, IeTime56 time) throws IOException {
        send(IEC60870Protocol.synchronizeClocks(originatorAddress, commonAddress, time));
    }

    public void synchronizeClocksResponse(int commonAddress, IeTime56 time) throws IOException {
        send(IEC60870Protocol.synchronizeClocksResponse(originatorAddress, commonAddress, time));
    }

    public void testCommand(int commonAddress) throws IOException {
        send(IEC60870Protocol.testCommand(originatorAddress, commonAddress));
    }

    
    public void testCommandWithTimeTag(int commonAddress, IeTestSequenceCounter testSequenceCounter, IeTime56 time) 
            throws IOException {
        send(IEC60870Protocol.testCommandWithTimeTag(originatorAddress, commonAddress, testSequenceCounter, time));
    }

    
    public void resetProcess(int commonAddress, int qualifierOfResetProcessCommand) throws IOException {
        send(IEC60870Protocol.resetProcess(originatorAddress, commonAddress, qualifierOfResetProcessCommand));
    }

    
    public void delayAcquisition(int commonAddress, int delayTime) throws IOException {
        send(IEC60870Protocol.delayAcquisition(originatorAddress, commonAddress, delayTime));
    }

    
    public void singleCommand(int commonAddress, CauseOfTransmission cot, int informationObjectAddress, 
            IeSingleCommand singleCommand) throws IOException {
        send(IEC60870Protocol.singleCommand(originatorAddress, commonAddress, cot, informationObjectAddress, singleCommand));
    }

    
    public void singleCommandWithTimeTag(int commonAddress, CauseOfTransmission cot, int informationObjectAddress,
            IeSingleCommand singleCommand, IeTime56 timeTag) throws IOException {
        send(IEC60870Protocol.singleCommandWithTimeTag(originatorAddress, commonAddress, cot, informationObjectAddress, 
                                                      singleCommand, timeTag));
    }

    public void doubleCommand(int commonAddress, CauseOfTransmission cot, int informationObjectAddress,
            IeDoubleCommand doubleCommand) throws IOException {
        send(IEC60870Protocol.doubleCommand(originatorAddress, commonAddress, cot, informationObjectAddress, doubleCommand));
    }

    
    public void doubleCommandWithTimeTag(int commonAddress, CauseOfTransmission cot, int informationObjectAddress,
            IeDoubleCommand doubleCommand, IeTime56 timeTag) throws IOException {
        send(IEC60870Protocol.doubleCommandWithTimeTag(originatorAddress, commonAddress, cot, informationObjectAddress, 
                                                      doubleCommand, timeTag));
    }

    
    public void regulatingStepCommand(int commonAddress, CauseOfTransmission cot, int informationObjectAddress,
            IeRegulatingStepCommand regulatingStep) throws IOException {
        send(IEC60870Protocol.regulatingStepCommand(originatorAddress, commonAddress, cot, informationObjectAddress, 
                                                   regulatingStep));
    }

    
    public void regulatingStepCommandWithTimeTag(int commonAddress, CauseOfTransmission cot, 
            int informationObjectAddress, IeRegulatingStepCommand regulatingStep, IeTime56 timeTag) throws IOException {
        send(IEC60870Protocol.regulatingStepCommandWithTimeTag(originatorAddress, commonAddress, cot, 
                                                              informationObjectAddress, regulatingStep, timeTag));
    }

    
    public void setNormalizedValueCommand(int commonAddress, CauseOfTransmission cot, int informationObjectAddress,
            IeNormalizedValue normalizedValue, IeQualifierOfSetPointCommand qualifier) throws IOException {
        send(IEC60870Protocol.setNormalizedValueCommand(originatorAddress, commonAddress, cot, informationObjectAddress,
                                                       normalizedValue, qualifier));
    }

    
    public void setNormalizedValueCommandWithTimeTag(int commonAddress, CauseOfTransmission cot, 
            int informationObjectAddress, IeNormalizedValue normalizedValue, 
            IeQualifierOfSetPointCommand qualifier, IeTime56 timeTag) throws IOException {
        send(IEC60870Protocol.setNormalizedValueCommandWithTimeTag(originatorAddress, commonAddress, cot, 
                                                                  informationObjectAddress, normalizedValue, 
                                                                  qualifier, timeTag));
    }

    
    public void setScaledValueCommand(int commonAddress, CauseOfTransmission cot, int informationObjectAddress,
            IeScaledValue scaledValue, IeQualifierOfSetPointCommand qualifier) throws IOException {
        send(IEC60870Protocol.setScaledValueCommand(originatorAddress, commonAddress, cot, informationObjectAddress,
                                                   scaledValue, qualifier));
    }

    
    public void setScaledValueCommandWithTimeTag(int commonAddress, CauseOfTransmission cot, 
            int informationObjectAddress, IeScaledValue scaledValue, 
            IeQualifierOfSetPointCommand qualifier, IeTime56 timeTag) throws IOException {
        send(IEC60870Protocol.setScaledValueCommandWithTimeTag(originatorAddress, commonAddress, cot,
                                                              informationObjectAddress, scaledValue,
                                                              qualifier, timeTag));
    }

    
    public void setShortFloatCommand(int commonAddress, CauseOfTransmission cot, int informationObjectAddress,
            IeShortFloat shortFloat, IeQualifierOfSetPointCommand qualifier) throws IOException {
        send(IEC60870Protocol.setShortFloatCommand(originatorAddress, commonAddress, cot, informationObjectAddress,
                                                  shortFloat, qualifier));
    }

    
    public void setShortFloatCommandWithTimeTag(int commonAddress, CauseOfTransmission cot, 
            int informationObjectAddress, IeShortFloat shortFloat, 
            IeQualifierOfSetPointCommand qualifier, IeTime56 timeTag) throws IOException {
        send(IEC60870Protocol.setShortFloatCommandWithTimeTag(originatorAddress, commonAddress, cot,
                                                             informationObjectAddress, shortFloat,
                                                             qualifier, timeTag));
    }

    
    public void bitstringCommand(int commonAddress, CauseOfTransmission cot, int informationObjectAddress,
            IeBinaryStateInformation binaryState) throws IOException {
        send(IEC60870Protocol.bitstringCommand(originatorAddress, commonAddress, cot, informationObjectAddress, binaryState));
    }

    
    public void bitstringCommandWithTimeTag(int commonAddress, CauseOfTransmission cot, int informationObjectAddress,
            IeBinaryStateInformation binaryState, IeTime56 timeTag) throws IOException {
        send(IEC60870Protocol.bitstringCommandWithTimeTag(originatorAddress, commonAddress, cot, informationObjectAddress, binaryState, timeTag));
    }

    
    public void parameterMeasuredValueNormalized(int commonAddress, int informationObjectAddress,
            IeNormalizedValue normalizedValue, IeQualifierOfParameterOfMeasuredValues qualifier) throws IOException {
        send(IEC60870Protocol.parameterNormalizedValueCommand(originatorAddress, commonAddress, informationObjectAddress, normalizedValue, qualifier));
    }

    
    public void parameterMeasuredValueScaled(int commonAddress, int informationObjectAddress,
            IeScaledValue scaledValue, IeQualifierOfParameterOfMeasuredValues qualifier) throws IOException {
        send(IEC60870Protocol.parameterScaledValueCommand(originatorAddress, commonAddress, informationObjectAddress, scaledValue, qualifier));
    }

    
    public void parameterMeasuredValueShortFloat(int commonAddress, int informationObjectAddress,
            IeShortFloat shortFloat, IeQualifierOfParameterOfMeasuredValues qualifier) throws IOException {
        send(IEC60870Protocol.parameterShortFloatCommand(originatorAddress, commonAddress, informationObjectAddress, shortFloat, qualifier));
    }

    
    public void parameterActivation(int commonAddress, CauseOfTransmission cot, int informationObjectAddress,
            int qualifierOfParameterActivation) throws IOException {
        send(IEC60870Protocol.parameterActivation(originatorAddress, commonAddress, cot, informationObjectAddress, qualifierOfParameterActivation));
    }
}
