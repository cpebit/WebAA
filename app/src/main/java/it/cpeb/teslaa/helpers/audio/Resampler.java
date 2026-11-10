package it.cpeb.teslaa.helpers.audio;

public class Resampler {

    public Resampler() {
    }

    /**
     * Do resampling. Currently the amplitude is stored by short such that maximum bitsPerSample is 16 (bytePerSample is 2)
     *
     * @param sourceData    The source data in bytes
     * @param bitsPerSample How many bits represents one sample (currently supports max. bitsPerSample=16)
     * @param sourceRate    Sample rate of the source data
     * @param targetRate    Sample rate of the target data
     * @return re-sampled data
     */
    public byte[] reSample(byte[] sourceData, int bitsPerSample, int sourceRate, int targetRate) {

        // make the bytes to amplitudes first
        int bytePerSample = bitsPerSample / 8;
        int numSamples = sourceData.length / bytePerSample;
        short[] amplitudes = new short[numSamples]; // 16 bit, use a short to store

        int pointer = 0;
        for (int i = 0; i < numSamples; i++) {
            short amplitude = 0;
            for (int byteNumber = 0; byteNumber < bytePerSample; byteNumber++) {
                // little endian
                amplitude |= (short) ((sourceData[pointer++] & 0xFF) << (byteNumber * 8));
            }
            amplitudes[i] = amplitude;
        }
        // end make the amplitudes

        // do interpolation
        LinearInterpolation reSample = new LinearInterpolation();
        short[] targetSample = reSample.interpolate(sourceRate, targetRate, amplitudes);
        int targetLength = targetSample.length;
        // end do interpolation

        // TODO: Remove the high frequency signals with a digital filter, leaving a signal containing only half-sample-rated frequency information, but still sampled at a rate of target sample rate. Usually FIR is used

        // end resample the amplitudes

        // convert the amplitude to bytes
        byte[] bytes;
        if (bytePerSample == 1) {
            bytes = new byte[targetLength];
            for (int i = 0; i < targetLength; i++) {
                bytes[i] = (byte) targetSample[i];
            }
        } else {
            // suppose bytePerSample==2
            bytes = new byte[targetLength * 2];
            for (int i = 0; i < targetSample.length; i++) {
                // little endian
                bytes[i * 2] = (byte) (targetSample[i] & 0xff);
                bytes[i * 2 + 1] = (byte) ((targetSample[i] >> 8) & 0xff);
            }
        }
        // end convert the amplitude to bytes

        return bytes;
    }

    public byte[] gainAdjust(byte[] audioSamples, float gain) {
        byte[] array = new byte[audioSamples.length];
        for (int i = 0; i < array.length; i += 2) {
            // convert byte pair to int
            short buf1 = audioSamples[i + 1];
            short buf2 = audioSamples[i];

            buf1 = (short) ((buf1 & 0xff) << 8);
            buf2 = (short) (buf2 & 0xff);

            short res = (short) (buf1 | buf2);
            res = (short) (res * gain);

            // convert back
            array[i] = (byte) res;
            array[i + 1] = (byte) (res >> 8);

        }
        return array;
    }

}
