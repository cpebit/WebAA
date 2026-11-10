#define MINIMP4_IMPLEMENTATION

#include "minimp4.h"
#include <cstring>
#include <jni.h>




#define VIDEO_FPS 30

MP4E_mux_t *mux= nullptr;
mp4_h26x_writer_t mp4wr;

// check switches

int sequential_mode = 1;
int fragmentation_mode = 1;
int is_hevc = 0;

JNIEnv *hack;



static int write_callback(int64_t offset, const void *buffer, size_t size, void *token)
{


    jbyteArray result=hack->NewByteArray(size);
    hack->SetByteArrayRegion(result, 0, size, static_cast<const jbyte *>(buffer));

    jclass cls = hack->FindClass("it/cpeb/teslaa/helpers/video/JpegDecoder");  // instead of FindClass
    jmethodID mid = hack->GetStaticMethodID(cls, "writeCallback", "([B)V");
    
    hack->CallStaticVoidMethod(cls, mid,result);

    return 0;
}

extern "C"
JNIEXPORT void JNICALL
Java_it_cpeb_teslaa_helpers_video_JpegDecoder_init(JNIEnv *env, jobject thiz, jint widh,jint height) {
    hack=env;

    mux = MP4E_open(sequential_mode, fragmentation_mode, thiz, &write_callback);


    if (MP4E_STATUS_OK != mp4_h26x_write_init(&mp4wr, mux, widh, height, is_hevc)) {
        printf("error: mp4_h26x_write_init failed\n");
        exit(1);
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_it_cpeb_teslaa_helpers_video_JpegDecoder_destroy(JNIEnv *env, jobject thiz) {
    if (env != hack)  //Fuck this, 3 hours going round in circles. enc can change leading to JNI crash!
        hack=env;
    try {
        MP4E_close(mux);
        mp4_h26x_write_close(&mp4wr);
    }
    catch (...) {

    }
}
extern "C"
JNIEXPORT void JNICALL
Java_it_cpeb_teslaa_helpers_video_JpegDecoder_write(JNIEnv *env, jobject thiz, jbyteArray data, jint nal_size) {
    if (env != hack)  //Fuck this, 3 hours going round in circles. enc can change leading to JNI crash!
         hack=env;

    jbyte buffer[nal_size];
    env->GetByteArrayRegion(data, 0, nal_size, buffer);




    if (MP4E_STATUS_OK !=
        mp4_h26x_write_nal(&mp4wr, reinterpret_cast<const unsigned char *>(buffer), nal_size,
                           90000 / VIDEO_FPS)) {
        printf("error: mp4_h26x_write_nal failed\n");
        exit(1);
    }
}