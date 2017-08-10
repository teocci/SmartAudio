/*
 * Copyright (C) 2011-2015 GUIGUI Simon, fyhertz@gmail.com
 *
 * This file is part of libstreaming (https://github.com/fyhertz/libstreaming)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.kseek.streaming.hw;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import android.annotation.SuppressLint;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.util.Log;

@SuppressLint("InlinedApi")
public class CodecManager
{

    public final static String TAG = "CodecManager";

    public static final int[] SUPPORTED_COLOR_FORMATS = {
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar,
            MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar
    };

    private static Codec[] sEncoders = null;
    private static Codec[] sDecoders = null;

    static class Codec
    {
        public Codec(String name, Integer[] formats)
        {
            this.name = name;
            this.formats = formats;
        }

        public String name;
        public Integer[] formats;
    }

    /**
     * Lists all encoders that claim to support a color format that we know how to use.
     *
     * @return A list of those encoders
     */
    @SuppressLint("NewApi")
    public synchronized static Codec[] findEncodersForMimeType(String mimeType)
    {
        if (sEncoders != null) return sEncoders;

        ArrayList<Codec> encoders = new ArrayList<Codec>();

        // We loop through the encoders, apparently this can take up to a sec (testes on a GS3)
        for (int j = MediaCodecList.getCodecCount() - 1; j >= 0; j--) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(j);
            if (!codecInfo.isEncoder()) continue;

            String[] types = codecInfo.getSupportedTypes();
            for (String type : types) {
                if (type.equalsIgnoreCase(mimeType)) {
                    try {
                        MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(mimeType);
                        Set<Integer> formats = new HashSet<Integer>();

                        // And through the color formats supported
                        for (int k = 0; k < capabilities.colorFormats.length; k++) {
                            int format = capabilities.colorFormats[k];

                            for (int l = 0; l < SUPPORTED_COLOR_FORMATS.length; l++) {
                                if (format == SUPPORTED_COLOR_FORMATS[l]) {
                                    formats.add(format);
                                }
                            }
                        }

                        Codec codec = new Codec(codecInfo.getName(), formats.toArray(new Integer[formats.size()]));
                        encoders.add(codec);
                    } catch (Exception e) {
                        Log.wtf(TAG, e);
                    }
                }
            }
        }

        sEncoders = (Codec[]) encoders.toArray(new Codec[encoders.size()]);
        return sEncoders;

    }

    /**
     * Lists all decoders that claim to support a color format that we know how to use.
     *
     * @return A list of those decoders
     */
    @SuppressLint("NewApi")
    public synchronized static Codec[] findDecodersForMimeType(String mimeType)
    {
        if (sDecoders != null) return sDecoders;
        ArrayList<Codec> decoders = new ArrayList<Codec>();

        // We loop through the decoders, apparently this can take up to a sec (testes on a GS3)
        for (int j = MediaCodecList.getCodecCount() - 1; j >= 0; j--) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(j);
            if (codecInfo.isEncoder()) continue;

            String[] types = codecInfo.getSupportedTypes();
            for (String type : types) {
                if (type.equalsIgnoreCase(mimeType)) {
                    try {
                        Set<Integer> formats = new HashSet<>();

                        // And through the color formats supported
                        formats.add(selectColorFormat(codecInfo, mimeType));
//						for (int k = 0; k < capabilities.colorFormats.length; k++) {
//							int format = capabilities.colorFormats[k];
//
//							for (int l=0;l<SUPPORTED_COLOR_FORMATS.length;l++) {
//								if (format == SUPPORTED_COLOR_FORMATS[l]) {
//									formats.add(format);
//								}
//							}
//						}

                        Codec codec = new Codec(codecInfo.getName(), formats.toArray(new Integer[formats.size()]));
                        decoders.add(codec);
                    } catch (Exception e) {
                        Log.wtf(TAG, e);
                    }
                }
            }
        }

        sDecoders = (Codec[]) decoders.toArray(new Codec[decoders.size()]);

        // We will use the decoder from google first, it seems to work properly on many phones
        for (int i = 0; i < sDecoders.length; i++) {
            if (sDecoders[i].name.equalsIgnoreCase("omx.google.h264.decoder")) {
                Codec codec = sDecoders[0];
                sDecoders[0] = sDecoders[i];
                sDecoders[i] = codec;
            }
        }

        return sDecoders;
    }


    /**
     * Returns a color format that is supported by the codec and by this test code.  If no
     * match is found, this throws a test failure -- the set of formats known to the test
     * should be expanded for new platforms.
     */
    public static int selectColorFormat(MediaCodecInfo codecInfo, String mimeType)
    {
        MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(mimeType);
        for (int i = 0; i < capabilities.colorFormats.length; i++) {
            int colorFormat = capabilities.colorFormats[i];
            if (isRecognizedFormat(colorFormat)) {
                return colorFormat;
            }
        }
        Log.e(TAG, "couldn't find a good color format for " + codecInfo.getName() + " / " + mimeType);
        return 0;   // not reached
    }

    /**
     * Returns true if this is a color format that this test code understands (i.e. we know how
     * to read and generate frames in this format).
     */
    public static boolean isRecognizedFormat(int colorFormat)
    {
        switch (colorFormat) {
            // these are the formats we know how to handle for this test
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:
            case MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar:
                return true;
            default:
                return false;
        }
    }

}

