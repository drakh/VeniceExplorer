package com.example.veniceexplorer;

import android.app.Activity;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.view.Surface;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.SurfaceTexture;
import android.os.Build;
import java.util.concurrent.locks.ReentrantLock;

public class VideoPlayerHelper implements OnPreparedListener, OnBufferingUpdateListener, OnCompletionListener, OnErrorListener{
    public static final int         CURRENT_POSITION            = -1;
    private MediaPlayer             mMediaPlayer                = null;
    private MEDIA_TYPE              mVideoType                  = MEDIA_TYPE.UNKNOWN;
    private SurfaceTexture          mSurfaceTexture             = null;
    private int                     mCurrentBufferingPercentage = 0;
    private String                  mMovieName                  = "";
    private byte                    mTextureID                  = 0;
    Intent                          mPlayerHelperActivityIntent = null;
    private Activity                mParentActivity             = null;
    private MEDIA_STATE             mCurrentState               = MEDIA_STATE.NOT_READY;
    private boolean                 mShouldPlayImmediately      = false;
    private int                     mSeekPosition               = CURRENT_POSITION;
    private ReentrantLock           mMediaPlayerLock            = null;
    private ReentrantLock           mSurfaceTextureLock         = null;
    public enum MEDIA_STATE {
        REACHED_END     (0),
        PAUSED          (1),
        STOPPED         (2),
        PLAYING         (3),
        READY           (4),
        NOT_READY       (5),
        ERROR           (6);

        private int type;
        MEDIA_STATE (int i)
        {
            this.type = i;
        }
        public int getNumericType()
        {
            return type;
        }
    }
    public enum MEDIA_TYPE {
        ON_TEXTURE              (0),
        FULLSCREEN              (1),
        ON_TEXTURE_FULLSCREEN   (2),
        UNKNOWN                 (3);

        private int type;
        MEDIA_TYPE (int i)
        {
            this.type = i;
        }
        public int getNumericType()
        {
            return type;
        }
    }
    
    public boolean init()
    {
        mMediaPlayerLock = new ReentrantLock();
        mSurfaceTextureLock = new ReentrantLock();

        return true;
    }
    
    public boolean deinit()
    {
        unload();

        mSurfaceTextureLock.lock();
            mSurfaceTexture = null;
        mSurfaceTextureLock.unlock();

        return true;
    }
    
    /** Loads a movie from a file in the assets folder */
    public boolean load(String filename, MEDIA_TYPE requestedType, boolean playOnTextureImmediately, int seekPosition)
    {
        // If the client requests that we should be able to play ON_TEXTURE then we need
        // to create a MediaPlayer

        boolean canBeOnTexture = false;
        boolean canBeFullscreen = false;

        boolean result = false;
        mMediaPlayerLock.lock();
        mSurfaceTextureLock.lock();

            // If the media has already been loaded then exit.
            // The client must first call unload() before calling load again
            if ((mCurrentState == MEDIA_STATE.READY) || (mMediaPlayer != null))
            {
                
            }
            else
            {
                if (((requestedType == MEDIA_TYPE.ON_TEXTURE) ||                        // If the client requests on texture only
                    (requestedType == MEDIA_TYPE.ON_TEXTURE_FULLSCREEN)) &&             // or on texture with full screen
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH))  // and this is an ICS device
                {
                    if (mSurfaceTexture == null)
                    {
                        
                    }
                    else
                    {
                        try
                        {
                            mMediaPlayer = new MediaPlayer();

                            // This example shows how to load the movie from the assets folder of the app
                            // However, if you would like to load the movie from the sdcard or from a network location
                            // simply comment the three lines below
                            AssetFileDescriptor afd = mParentActivity.getAssets().openFd(filename);
                            mMediaPlayer.setDataSource(afd.getFileDescriptor(),afd.getStartOffset(),afd.getLength());
                            afd.close();

                            // and uncomment this one
                            // mMediaPlayer.setDataSource("/sdcard/myMovie.m4v");

                            mMediaPlayer.prepareAsync();
                            mMediaPlayer.setOnPreparedListener(this);
                            mMediaPlayer.setOnBufferingUpdateListener(this);
                            mMediaPlayer.setOnCompletionListener(this);
                            mMediaPlayer.setOnErrorListener(this);
                            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                            mMediaPlayer.setSurface(new Surface(mSurfaceTexture));
                            canBeOnTexture = true;
                            mShouldPlayImmediately = playOnTextureImmediately;
                        }
                        catch (Exception e)
                        {
                            
                            
                            mCurrentState = MEDIA_STATE.ERROR; 
                            mMediaPlayerLock.unlock();
                            mSurfaceTextureLock.unlock();
                            return false;
                        }
                    }
                }
                else
                {
                    try
                    {
                        // We need to verify that the file exists
                        AssetFileDescriptor afd = mParentActivity.getAssets().openFd(filename);
                        afd.close();
                    }
                    catch (Exception e)
                    {
                        
                        mCurrentState = MEDIA_STATE.ERROR; 
                        mMediaPlayerLock.unlock();
                        mSurfaceTextureLock.unlock();
                        return false;
                    }
                }

                // If the client requests that we should be able to play FULLSCREEN
                // then we need to create a FullscreenPlaybackActivity
                if ((requestedType == MEDIA_TYPE.FULLSCREEN) || (requestedType == MEDIA_TYPE.ON_TEXTURE_FULLSCREEN))
                {
                }

                // We store the parameters for further use
                mMovieName = filename;
                mSeekPosition = seekPosition;

                if (canBeFullscreen && canBeOnTexture)  mVideoType = MEDIA_TYPE.ON_TEXTURE_FULLSCREEN;
                else if (canBeFullscreen) {             mVideoType = MEDIA_TYPE.FULLSCREEN; mCurrentState = MEDIA_STATE.READY; } // If it is pure fullscreen then we're ready otherwise we let the MediaPlayer load first
                else if (canBeOnTexture)                mVideoType = MEDIA_TYPE.ON_TEXTURE;
                else                                    mVideoType = MEDIA_TYPE.UNKNOWN;

                result = true;
            }

        mSurfaceTextureLock.unlock();
        mMediaPlayerLock.unlock();

        return result;
    }
    public boolean unload()
    {
        mMediaPlayerLock.lock();
            if (mMediaPlayer != null) {
                try 
                {
                    mMediaPlayer.stop();
                } 
                catch (Exception e) 
                {
                    mMediaPlayerLock.unlock();
                    
                }
                        
                mMediaPlayer.release();
                mMediaPlayer = null;
            }
        mMediaPlayerLock.unlock();

        mCurrentState = MEDIA_STATE.NOT_READY;
        mVideoType = MEDIA_TYPE.UNKNOWN;
        return true;
    }
    public boolean isPlayableOnTexture()
    {
        if ((mVideoType == MEDIA_TYPE.ON_TEXTURE) || (mVideoType == MEDIA_TYPE.ON_TEXTURE_FULLSCREEN))
            return true;

        return false;
    }

    /** Indicates whether the movie can be played fullscreen */
    public boolean isPlayableFullscreen()
    {
        if ((mVideoType == MEDIA_TYPE.FULLSCREEN) || (mVideoType == MEDIA_TYPE.ON_TEXTURE_FULLSCREEN))
            return true;

        return false;
    }
    MEDIA_STATE getStatus()
    {
        return mCurrentState;
    }

    /** Returns the width of the video frame */
    public int getVideoWidth()
    {
        if (!isPlayableOnTexture())
        {
            // DebugLog.LOGD("Cannot get the video width if it is not playable on texture");
            return -1;
        }

        if ((mCurrentState == MEDIA_STATE.NOT_READY) || (mCurrentState == MEDIA_STATE.ERROR))
        {
            // DebugLog.LOGD("Cannot get the video width if it is not ready");
            return -1;
        }

        int result=-1;
        mMediaPlayerLock.lock();
            if (mMediaPlayer != null)
                result = mMediaPlayer.getVideoWidth();
        mMediaPlayerLock.unlock();

        return result;
    }

    /** Returns the height of the video frame */
    public int getVideoHeight()
    {
        if (!isPlayableOnTexture())
        {
            // DebugLog.LOGD("Cannot get the video height if it is not playable on texture");
            return -1;
        }

        if ((mCurrentState == MEDIA_STATE.NOT_READY) || (mCurrentState == MEDIA_STATE.ERROR))
        {
            // DebugLog.LOGD("Cannot get the video height if it is not ready");
            return -1;
        }

        int result=-1;
        mMediaPlayerLock.lock();
            if (mMediaPlayer != null)
                result = mMediaPlayer.getVideoHeight();
        mMediaPlayerLock.unlock();

        return result;
    }

    /** Returns the length of the current movie */
    public float getLength()
    {
        if (!isPlayableOnTexture())
        {
            // DebugLog.LOGD("Cannot get the video length if it is not playable on texture");
            return -1;
        }

        if ((mCurrentState == MEDIA_STATE.NOT_READY) || (mCurrentState == MEDIA_STATE.ERROR))
        {
            // DebugLog.LOGD("Cannot get the video length if it is not ready");
            return -1;
        }


        int result=-1;
        mMediaPlayerLock.lock();
            if (mMediaPlayer != null)
                result = mMediaPlayer.getDuration()/1000;
        mMediaPlayerLock.unlock();

        return result;
    }


    /** Request a movie to be played either full screen or on texture and at a given position */
    public boolean play(boolean fullScreen, int seekPosition)
    {
        if (fullScreen)
        {
            // If the play request was for fullscreen playback
            // We check first whether this was requested upon load time
            if (!isPlayableFullscreen())
            {
                
                return false;
            }

            if (isPlayableOnTexture())
            {
                // If it can also play on texture then we forward information such as whether
                // it is currently playing (shouldPlayImmediately) and in which position
                // it was being played previously (currentSeekPosition)

                mMediaPlayerLock.lock();

                    if (mMediaPlayer == null)
                    {
                        mMediaPlayerLock.unlock();
                        return false;
                    }

                    if (mMediaPlayer.isPlaying())
                        mPlayerHelperActivityIntent.putExtra("shouldPlayImmediately", true);
                    else
                        mPlayerHelperActivityIntent.putExtra("shouldPlayImmediately", false);

                    try 
                    {
                        mMediaPlayer.pause();
                    } 
                    catch (Exception e) 
                    {
                        mMediaPlayerLock.unlock();
                        
                    }
                    

                    if (seekPosition != CURRENT_POSITION)
                        mPlayerHelperActivityIntent.putExtra("currentSeekPosition", seekPosition);
                    else
                        mPlayerHelperActivityIntent.putExtra("currentSeekPosition", mMediaPlayer.getCurrentPosition());

                mMediaPlayerLock.unlock();
            }
            else
            {
                // If it cannot play on texture then we set these values to default
                mPlayerHelperActivityIntent.putExtra("currentSeekPosition", 0);
                mPlayerHelperActivityIntent.putExtra("shouldPlayImmediately", true);

                if (seekPosition != CURRENT_POSITION)
                    mPlayerHelperActivityIntent.putExtra("currentSeekPosition", seekPosition);
                else
                    mPlayerHelperActivityIntent.putExtra("currentSeekPosition", 0);
            }

            // We must pass the current playback orientation of the activity
            // and the name of the movie currently being played
            mPlayerHelperActivityIntent.putExtra("requestedOrientation", mParentActivity.getRequestedOrientation());
            mPlayerHelperActivityIntent.putExtra("movieName", mMovieName);
            mParentActivity.startActivityForResult(mPlayerHelperActivityIntent,1);
            return true;
        }
        else
        {
            // If the client requested playback on texture
            // we must first verify that it is possible
            if (!isPlayableOnTexture())
            {
                
                return false;
            }

            if ((mCurrentState == MEDIA_STATE.NOT_READY) || (mCurrentState == MEDIA_STATE.ERROR))
            {
                
                return false;
            }

            mMediaPlayerLock.lock();
                // If the client requests a given position
                if (seekPosition != CURRENT_POSITION)
                {
                    try 
                    {
                        mMediaPlayer.seekTo(seekPosition);
                    } 
                    catch (Exception e) 
                    {
                        mMediaPlayerLock.unlock();
                        
                    }
                }
                else    // If it had reached the end loop it back
                    if (mCurrentState == MEDIA_STATE.REACHED_END)
                    {
                        try 
                        {
                            mMediaPlayer.seekTo(0);
                        } 
                        catch (Exception e) 
                        {
                            mMediaPlayerLock.unlock();
                            
                        }
                    }

                // Then simply start playing
                try 
                {
                    mMediaPlayer.start();
                } 
                catch (Exception e) 
                {
                    mMediaPlayerLock.unlock();
                    
                }
                mCurrentState = MEDIA_STATE.PLAYING;

            mMediaPlayerLock.unlock();

            return true;
        }
    }

    /** Pauses the current movie being played */
    public boolean pause()
    {
        if (!isPlayableOnTexture())
        {
            // DebugLog.LOGD("Cannot pause this video since it is not on texture");
            return false;
        }

        if ((mCurrentState == MEDIA_STATE.NOT_READY) || (mCurrentState == MEDIA_STATE.ERROR))
        {
            // DebugLog.LOGD("Cannot pause this video if it is not ready");
            return false;
        }

        boolean result = false;

        mMediaPlayerLock.lock();
            if (mMediaPlayer != null)
            {
                if (mMediaPlayer.isPlaying())
                {
                    try 
                    {
                        mMediaPlayer.pause();
                    } 
                    catch (Exception e) 
                    {
                        mMediaPlayerLock.unlock();
                        
                    }
                    mCurrentState = MEDIA_STATE.PAUSED;
                    result = true;
                }
            }
        mMediaPlayerLock.unlock();

        return result;
    }

    /** Stops the current movie being played */
    public boolean stop()
    {
        if (!isPlayableOnTexture())
        {
            // DebugLog.LOGD("Cannot stop this video since it is not on texture");
            return false;
        }

        if ((mCurrentState == MEDIA_STATE.NOT_READY) || (mCurrentState == MEDIA_STATE.ERROR))
        {
            // DebugLog.LOGD("Cannot stop this video if it is not ready");
            return false;
        }

        boolean result = false;

        mMediaPlayerLock.lock();
            if (mMediaPlayer != null)
            {
                mCurrentState = MEDIA_STATE.STOPPED;
                try 
                {
                    mMediaPlayer.stop();
                } 
                catch (Exception e) 
                {
                    mMediaPlayerLock.unlock();
                    
                }
                
                result = true;
            }
        mMediaPlayerLock.unlock();

        return result;
    }

    /** Tells the VideoPlayerHelper to update the data from the video feed */
    public byte updateVideoData()
    {
        if (!isPlayableOnTexture())
        {
            // DebugLog.LOGD("Cannot update the data of this video since it is not on texture");
            return -1;
        }

        byte result = -1;

        mSurfaceTextureLock.lock();
            if (mSurfaceTexture != null)
            {
                // Only request an update if currently playing
                if (mCurrentState == MEDIA_STATE.PLAYING)
                    mSurfaceTexture.updateTexImage();

                result = mTextureID;
            }
        mSurfaceTextureLock.unlock();

        return result;
    }

    /** Moves the movie to the requested seek position */
    public boolean seekTo(int position)
    {
        if (!isPlayableOnTexture())
        {
            // DebugLog.LOGD("Cannot seek-to on this video since it is not on texture");
            return false;
        }

        if ((mCurrentState == MEDIA_STATE.NOT_READY) || (mCurrentState == MEDIA_STATE.ERROR))
        {
            // DebugLog.LOGD("Cannot seek-to on this video if it is not ready");
            return false;
        }

        boolean result = false;
        mMediaPlayerLock.lock();
            if (mMediaPlayer != null)
            {
                try 
                {
                    mMediaPlayer.seekTo(position);
                } 
                catch (Exception e) 
                {
                    mMediaPlayerLock.unlock();
                }
                result = true;
            }
        mMediaPlayerLock.unlock();

        return result;
    }

    /** Gets the current seek position */
    public int getCurrentPosition()
    {
        if (!isPlayableOnTexture())
        {
            // DebugLog.LOGD("Cannot get the current playback position of this video since it is not on texture");
            return -1;
        }

        if ((mCurrentState == MEDIA_STATE.NOT_READY) || (mCurrentState == MEDIA_STATE.ERROR))
        {
            // DebugLog.LOGD("Cannot get the current playback position of this video if it is not ready");
            return -1;
        }

        int result = -1;
        mMediaPlayerLock.lock();
            if (mMediaPlayer != null)
                result = mMediaPlayer.getCurrentPosition();
        mMediaPlayerLock.unlock();

        return result;
    }

    /** Sets the volume of the movie to the desired value */
    public boolean setVolume(float value)
    {
        if (!isPlayableOnTexture())
        {
            // DebugLog.LOGD("Cannot set the volume of this video since it is not on texture");
            return false;
        }

        if ((mCurrentState == MEDIA_STATE.NOT_READY) || (mCurrentState == MEDIA_STATE.ERROR))
        {
            // DebugLog.LOGD("Cannot set the volume of this video if it is not ready");
            return false;
        }

        boolean result = false;
        mMediaPlayerLock.lock();
            if (mMediaPlayer != null)
            {
                mMediaPlayer.setVolume(value, value);
                result = true;
            }
        mMediaPlayerLock.unlock();

        return result;
    }

    /**
     *  The following functions are specific to Android
     *  and will likely not be implemented on other platforms
     */

    /** Gets the buffering percentage in case the movie is loaded from network */
    public int getCurrentBufferingPercentage()
    {
        return mCurrentBufferingPercentage;
    }

    /** Listener call for buffering */
    public void onBufferingUpdate(MediaPlayer arg0, int arg1) {
        //DebugLog.LOGD("onBufferingUpdate " + arg1);

        mMediaPlayerLock.lock();
            if (mMediaPlayer != null)
            {
                if (arg0 == mMediaPlayer)
                    mCurrentBufferingPercentage = arg1;
            }
        mMediaPlayerLock.unlock();
    }

    /** With this we can set the parent activity */
    public void setActivity(Activity newActivity)
    {
        mParentActivity = newActivity;
    }

    /** To set a value upon completion */
    public void onCompletion(MediaPlayer arg0) {
        // Signal that the video finished playing
        mCurrentState = MEDIA_STATE.REACHED_END;
    }

    /** Used to set up the surface texture*/
    public boolean setupSurfaceTexture(int nativeTextureID)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
        {
            // We create a surface texture where the video can be played
            // We have to give it a texture id of an already created (in native) // OpenGL texture
            mSurfaceTextureLock.lock();
                mSurfaceTexture = new SurfaceTexture(nativeTextureID);
                mTextureID = (byte)nativeTextureID;
            mSurfaceTextureLock.unlock();

            return true;
        }
        else
            return false;
    }

    public void getSurfaceTextureTransformMatrix(float []mtx)
    {
        mSurfaceTextureLock.lock();
            if (mSurfaceTexture != null)
                mSurfaceTexture.getTransformMatrix(mtx);
        mSurfaceTextureLock.unlock();
    }

    /** This is called when the movie is ready for playback */
    public void onPrepared(MediaPlayer mediaplayer)
    {
        mCurrentState = MEDIA_STATE.READY;

        // If requested an immediate play
        if (mShouldPlayImmediately)
            play(false, mSeekPosition);

        mSeekPosition = 0;
    }

    public boolean onError(MediaPlayer mp, int what, int extra) {
        
        if (mp == mMediaPlayer)
        {
            String errorDescription;
            
            switch (what) 
            {
                case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                    errorDescription = "The video is streamed and its container is not valid for progressive playback";
                    break;
                case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                    errorDescription = "Media server died";
                    break;
                case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                    errorDescription = "Unspecified media player error";
                    break;
                default:
                    errorDescription = "Unknown error " + what;
            }    
            mCurrentState = MEDIA_STATE.ERROR;
            
            unload();

            return true;
        }
        
        return false;
        
    }
}
