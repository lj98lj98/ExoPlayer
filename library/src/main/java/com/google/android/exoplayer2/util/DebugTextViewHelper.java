/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.exoplayer2.util;

import com.google.android.exoplayer2.CodecCounters;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.upstream.BandwidthMeter;

import android.widget.TextView;

/**
 * A helper class for periodically updating a {@link TextView} with debug information obtained from
 * a {@link SimpleExoPlayer}.
 */
public final class DebugTextViewHelper implements Runnable, ExoPlayer.EventListener {

  private static final int REFRESH_INTERVAL_MS = 1000;

  private final SimpleExoPlayer player;
  private final TextView textView;

  private boolean started;

  /**
   * @param player The {@link SimpleExoPlayer} from which debug information should be obtained.
   * @param textView The {@link TextView} that should be updated to display the information.
   */
  public DebugTextViewHelper(SimpleExoPlayer player, TextView textView) {
    this.player = player;
    this.textView = textView;
  }

  /**
   * Starts periodic updates of the {@link TextView}.
   * <p>
   * Should be called from the application's main thread.
   */
  public void start() {
    if (started) {
      return;
    }
    started = true;
    player.addListener(this);
    run();
  }

  /**
   * Stops periodic updates of the {@link TextView}.
   * <p>
   * Should be called from the application's main thread.
   */
  public void stop() {
    if (!started) {
      return;
    }
    started = false;
    player.removeListener(this);
    textView.removeCallbacks(this);
  }

  @Override
  public void run() {
    updateTextView();
    textView.postDelayed(this, REFRESH_INTERVAL_MS);
  }

  private void updateTextView() {
    textView.setText(getPlayerStateString() + getPlayerSourceIndexString() + getBandwidthString()
        + getVideoString() + getAudioString());
  }

  private String getPlayerStateString() {
    String text = "playWhenReady:" + player.getPlayWhenReady() + " playbackState:";
    switch(player.getPlaybackState()) {
      case ExoPlayer.STATE_BUFFERING:
        text += "buffering";
        break;
      case ExoPlayer.STATE_ENDED:
        text += "ended";
        break;
      case ExoPlayer.STATE_IDLE:
        text += "idle";
        break;
      case ExoPlayer.STATE_READY:
        text += "ready";
        break;
      default:
        text += "unknown";
        break;
    }
    return text;
  }

  private String getPlayerSourceIndexString() {
    return " source:" + player.getCurrentSourceIndex();
  }

  private String getBandwidthString() {
    BandwidthMeter bandwidthMeter = player.getBandwidthMeter();
    if (bandwidthMeter == null
        || bandwidthMeter.getBitrateEstimate() == BandwidthMeter.NO_ESTIMATE) {
      return " bw:?";
    } else {
      return " bw:" + (bandwidthMeter.getBitrateEstimate() / 1000);
    }
  }

  private String getVideoString() {
    Format format = player.getVideoFormat();
    if (format == null) {
      return "";
    }
    return "\n" + format.sampleMimeType + "(id:" + format.id + " r:" + format.width + "x"
        + format.height + getCodecCounterBufferCountString(player.getVideoCodecCounters()) + ")";
  }

  private String getAudioString() {
    Format format = player.getAudioFormat();
    if (format == null) {
      return "";
    }
    return "\n" + format.sampleMimeType + "(id:" + format.id + " hz:" + format.sampleRate + " ch:"
        + format.channelCount + getCodecCounterBufferCountString(player.getAudioCodecCounters())
        + ")";
  }

  private static String getCodecCounterBufferCountString(CodecCounters counters) {
    if (counters == null) {
      return "";
    }
    counters.ensureUpdated();
    return " rb:" + counters.renderedOutputBufferCount
        + " sb:" + counters.skippedOutputBufferCount
        + " db:" + counters.droppedOutputBufferCount
        + " mcdb:" + counters.maxConsecutiveDroppedOutputBufferCount;
  }

  // ExoPlayer.EventListener implementation

  @Override
  public void onLoadingChanged(boolean isLoading) {
    // Do nothing.
  }

  @Override
  public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
    updateTextView();
  }

  @Override
  public void onPlayWhenReadyCommitted() {
    // Do nothing.
  }

  @Override
  public void onPositionDiscontinuity(int sourceIndex, long positionMs) {
    updateTextView();
  }

  @Override
  public void onPlayerError(ExoPlaybackException error) {
    // Do nothing.
  }

}