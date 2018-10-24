package cafe.adriel.androidaudioconverter;

import android.content.Context;
import android.util.Log;

import com.github.hiteshsondhi88.libffmpeg.FFmpeg;
import com.github.hiteshsondhi88.libffmpeg.FFmpegExecuteResponseHandler;
import com.github.hiteshsondhi88.libffmpeg.FFmpegLoadBinaryResponseHandler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import cafe.adriel.androidaudioconverter.callback.IConvertCallback;
import cafe.adriel.androidaudioconverter.callback.ILoadCallback;
import cafe.adriel.androidaudioconverter.model.AudioFormat;

public class AndroidAudioConverter {
    private static final String LOG_TAG = "AudioConverter";
    private static boolean loaded;

    private Context context;
    private File inputFile;
    private File outputFile;
    private AudioFormat format;
    private IConvertCallback callback;

    private AndroidAudioConverter(Context context){
        this.context = context;
    }

    public static boolean isLoaded(){
        return loaded;
    }

    public static void load(Context context, final ILoadCallback callback){
        try {
            FFmpeg.getInstance(context).loadBinary(new FFmpegLoadBinaryResponseHandler() {
                        @Override
                        public void onStart() {

                        }

                        @Override
                        public void onSuccess() {
                            loaded = true;
                            callback.onSuccess();
                        }

                        @Override
                        public void onFailure() {
                            loaded = false;
                            callback.onFailure(new Exception("Failed to loaded FFmpeg lib"));
                        }

                        @Override
                        public void onFinish() {

                        }
                    });
        } catch (Exception e){
            loaded = false;
            callback.onFailure(e);
        }
    }

    public static AndroidAudioConverter with(Context context) {
        return new AndroidAudioConverter(context);
    }

    public AndroidAudioConverter setInputFile(File originalFile) {
        this.inputFile = originalFile;
        return this;
    }

    public AndroidAudioConverter setFormat(AudioFormat format) {
        this.format = format;
        return this;
    }

    public AndroidAudioConverter setCallback(IConvertCallback callback) {
        this.callback = callback;
        return this;
    }

    public void convert() {
        if(!isLoaded()){
            callback.onFailure(new Exception("FFmpeg not loaded"));
            return;
        }
        if(inputFile == null || !inputFile.exists()){
            callback.onFailure(new IOException("File not exists"));
            return;
        }
        if(!inputFile.canRead()){
            callback.onFailure(new IOException("Can't read the file. Missing permission?"));
            return;
        }

        final File convertedFile = getConvertedFile(format);
        final String[] cmd = new String[]{
                "-y",
                "-i", inputFile.getPath(),
                "-sample_fmt", "s16",
                "-ac", "1",
                convertedFile.getPath()};

        try {
            FFmpeg.getInstance(context).execute(cmd, new FFmpegExecuteResponseHandler() {
                        @Override
                        public void onStart() {

                        }

                        @Override
                        public void onProgress(String message) {

                        }

                        @Override
                        public void onSuccess(String message) {
                            callback.onSuccess(convertedFile);
                        }

                        @Override
                        public void onFailure(String message) {
                            callback.onFailure(new IOException(message));
                        }

                        @Override
                        public void onFinish() {

                        }
                    });
        } catch (Exception e){
            callback.onFailure(e);
        }
    }

    private static File getConvertedFile(AudioFormat format){
        try {
            Path convertedFilePath = Files.createTempFile("converted_", "."+ format.getFormat());
            return new File(convertedFilePath.toString());
        }
        catch(IOException ex)
        {
            Log.e(LOG_TAG, "Cannot create temporary file for conversion");
            System.exit(0);
        }
        catch(Exception ex)
        {
            Log.e(LOG_TAG, "Unknown exception, abort");
            System.exit(0);
        }

        return null;
    }
}