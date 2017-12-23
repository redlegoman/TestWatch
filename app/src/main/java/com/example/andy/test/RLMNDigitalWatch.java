package com.example.andy.test;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Bitmap;

import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.view.SurfaceHolder;
import android.view.WindowInsets;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Digital watch face with seconds. In ambient mode, the seconds aren't displayed. On devices with
 * low-bit ambient mode, the text is drawn without anti-aliasing in ambient mode.
 * <p>
 * Important Note: Because watch face apps do not have a default Activity in
 * their project, you will need to set your Configurations to
 * "Do not launch Activity" for both the Wear and/or Application modules. If you
 * are unsure how to do this, please review the "Run Starter project" section
 * in the Google Watch Face Code Lab:
 * https://codelabs.developers.google.com/codelabs/watchface/index.html#0
 */
public class RLMNDigitalWatch extends CanvasWatchFaceService {
    private static final Typeface NORMAL_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);
    private static final Typeface BOLD_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);
    /**
     * Update rate in milliseconds for interactive mode. Defaults to one second
     * because the watch face needs to update seconds in interactive mode.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<RLMNDigitalWatch.Engine> mWeakReference;

        public EngineHandler(RLMNDigitalWatch.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            RLMNDigitalWatch.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    private class Engine extends CanvasWatchFaceService.Engine {

        private final Handler mUpdateTimeHandler = new EngineHandler(this);
        private Calendar mCalendar;
        private final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };
        private boolean mRegisteredTimeZoneReceiver = false;
        private float mXOffset;
        private float mYOffset;

        private float mDayXOffset;
        private float mDayYOffset;

        private Paint mTextPaint;

        private Bitmap mBackgroundBitmap;



        private Paint mBackgroundPaint;
        private Paint mTextHourPaint;
        private Paint mTextMinutePaint;
        private Paint mTextSecondPaint;

        private Paint mTextDayPaint;
        private Paint mTextDatePaint;
        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        private boolean mLowBitAmbient;
        private boolean mBurnInProtection;
        private boolean mAmbient;

        private float mHourHandLength;
        private float mMinuteHandLength;
        private float mSecondHandLength;

        private int mWidth;
        private int mHeight;
        private float mCenterX;
        private float mCenterY;
        private float mScale = 1;


        @Override
        public void onCreate(SurfaceHolder holder) {




            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(RLMNDigitalWatch.this)
                    .setAcceptsTapEvents(true)
                    .build());

            final int backgroundResId = R.drawable.grey2;

            mBackgroundBitmap = BitmapFactory.decodeResource(getResources(), backgroundResId);

            mCalendar = Calendar.getInstance();

            Resources resources = RLMNDigitalWatch.this.getResources();
            mYOffset = resources.getDimension(R.dimen.digital_y_offset);
            mDayYOffset = resources.getDimension(R.dimen.digital_day_y_offset);
            // Initializes background.
            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(
                    ContextCompat.getColor(getApplicationContext(), R.color.background));

// Initializes Watch Face.
            mTextPaint = new Paint();
            mTextPaint.setTypeface(NORMAL_TYPEFACE);
            mTextPaint.setAntiAlias(true);
            mTextPaint.setColor(
                    ContextCompat.getColor(getApplicationContext(), R.color.digital_text));

            // Initializes Watch Face.
            mTextHourPaint = new Paint();
            mTextHourPaint.setTypeface(BOLD_TYPEFACE);
            mTextHourPaint.setAntiAlias(true);
            mTextHourPaint.setColor(
                    ContextCompat.getColor(getApplicationContext(), R.color.digital_hour_text));

            mTextMinutePaint = new Paint();
            mTextMinutePaint.setTypeface(NORMAL_TYPEFACE);
            mTextMinutePaint.setAntiAlias(true);
            mTextMinutePaint.setColor(
                    ContextCompat.getColor(getApplicationContext(), R.color.digital_minute_text));

            mTextSecondPaint = new Paint();
            mTextSecondPaint.setTypeface(NORMAL_TYPEFACE);
            mTextSecondPaint.setAntiAlias(true);
            mTextSecondPaint.setColor(
                    ContextCompat.getColor(getApplicationContext(), R.color.digital_second_text));

            mTextDayPaint = new Paint();
            mTextDayPaint.setTypeface(NORMAL_TYPEFACE);
            mTextDayPaint.setAntiAlias(true);
            mTextDayPaint.setColor(ContextCompat.getColor(getApplicationContext(), R.color.digital_day_text));

            mTextDatePaint = new Paint();
            mTextDatePaint.setTypeface(NORMAL_TYPEFACE);
            mTextDatePaint.setAntiAlias(true);
            mTextDatePaint.setColor(ContextCompat.getColor(getApplicationContext(), R.color.digital_date_text));
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            } else {
                unregisterReceiver();
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            RLMNDigitalWatch.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            RLMNDigitalWatch.this.unregisterReceiver(mTimeZoneReceiver);
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            // Load resources that have alternate values for round watches.
            Resources resources = RLMNDigitalWatch.this.getResources();
            boolean isRound = insets.isRound();
            mXOffset = resources.getDimension(isRound
                    ? R.dimen.digital_x_offset_round : R.dimen.digital_x_offset);
            mDayXOffset = resources.getDimension(isRound
                    ? R.dimen.digital_day_x_offset_round : R.dimen.digital_day_x_offset);
            float textHourSize = resources.getDimension(isRound
                    ? R.dimen.digital_hour_text_size_round : R.dimen.digital_hour_text_size);
            float textMinuteSize = resources.getDimension(isRound
                    ? R.dimen.digital_minute_text_size_round : R.dimen.digital_minute_text_size);
            float textSecondSize = resources.getDimension(isRound
                    ? R.dimen.digital_second_text_size_round : R.dimen.digital_second_text_size);
            float textDaySize = resources.getDimension(isRound
                    ? R.dimen.digital_day_text_size_round : R.dimen.digital_day_text_size);
            float textDateSize = resources.getDimension(isRound
                    ? R.dimen.digital_date_text_size_round : R.dimen.digital_date_text_size);

            float textSize = resources.getDimension(isRound
                    ? R.dimen.digital_text_size_round : R.dimen.digital_text_size);

            mTextPaint.setTextSize(textSize);

            mTextHourPaint.setTextSize(textHourSize);
            mTextMinutePaint.setTextSize(textMinuteSize);
            mTextSecondPaint.setTextSize(textSecondSize);
            mTextDayPaint.setTextSize(textDaySize);
            mTextDatePaint.setTextSize(textDateSize);
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            mBurnInProtection = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);

            mAmbient = inAmbientMode;
            if (mLowBitAmbient) {
                mTextPaint.setAntiAlias(!inAmbientMode);

                mTextHourPaint.setAntiAlias(!inAmbientMode);
                mTextMinutePaint.setAntiAlias(!inAmbientMode);
                mTextDayPaint.setAntiAlias(!inAmbientMode);
                mTextDatePaint.setAntiAlias(!inAmbientMode);
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        /**
         * Captures tap event (and tap type) and toggles the background color if the user finishes
         * a tap.
         */
        @Override
        public void onTapCommand(int tapType, int x, int y, long eventTime) {
            switch (tapType) {
                case TAP_TYPE_TOUCH:
                    // The user has started touching the screen.
                    break;
                case TAP_TYPE_TOUCH_CANCEL:
                    // The user has started a different gesture or otherwise cancelled the tap.
                    //String timetext = Objects.toString(eventTime);
                    //Toast.makeText(getApplicationContext(), R.string.message_long , Toast.LENGTH_SHORT).show();
                    break;
                case TAP_TYPE_TAP:
                    // The user has completed the tap gesture.
                    // TODO: Add code to handle the tap gesture.
                    //timetext = Objects.toString(eventTime);
                    //Toast.makeText(getApplicationContext(), R.string.message , Toast.LENGTH_SHORT).show();
                    break;
            }
            invalidate();
        }


        private String formatTwoDigitNumber(int hour) {
            return String.format("%02d", hour);
        }

        boolean is24Hour = true;

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);
            mWidth = width;
            mHeight = height;
            /*
             * Find the coordinates of the center point on the screen.
             * Ignore the window insets so that, on round watches
             * with a "chin", the watch face is centered on the entire screen,
             * not just the usable portion.
             */
            mCenterX = mWidth / 2f;
            mCenterY = mHeight / 2f;
            mScale = ((float) width) / (float) mBackgroundBitmap.getWidth();
            /*
             * Calculate the lengths of the watch hands and store them in member variables.
             */
            mHourHandLength = mCenterX - 80;
            mMinuteHandLength = mCenterX - 40;
            mSecondHandLength = mCenterX - 20;

            mBackgroundBitmap = Bitmap.createScaledBitmap(mBackgroundBitmap,
                    (int) (mBackgroundBitmap.getWidth() * mScale),
                    (int) (mBackgroundBitmap.getHeight() * mScale), true);
        }


        @Override
        public void onDraw(Canvas canvas, Rect bounds) {


            // Draw the background.
            if (isInAmbientMode()) {
                canvas.drawColor(Color.BLACK);
            } else {
                canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);
            }
            long now = System.currentTimeMillis();
            // Draw H:MM in ambient mode or H:MM:SS in interactive mode.
            Date mDate = new Date(now);
            SimpleDateFormat mDayOfWeekFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
            SimpleDateFormat mDateFormat = new SimpleDateFormat("d MMMM",Locale.getDefault());

            //String thedate = (mDateFormat.format(mDate));


            mCalendar.setTimeInMillis(now);
            String text = mAmbient
                    ? String.format("%d:%02d", mCalendar.get(Calendar.HOUR),
                    mCalendar.get(Calendar.MINUTE))
                    : String.format("%d:%02d:%02d", mCalendar.get(Calendar.HOUR),
                    mCalendar.get(Calendar.MINUTE), mCalendar.get(Calendar.SECOND));
            //canvas.drawText(text, mXOffset, mYOffset, mTextPaint);

            String hour = String.format("%02d", mCalendar.get(Calendar.HOUR)) + " ";
            if (is24Hour) {
                hour = formatTwoDigitNumber(mCalendar.get(Calendar.HOUR_OF_DAY));
            } else {
                int houri = mCalendar.get(Calendar.HOUR);
                if (houri == 0) {
                    houri = 12;
                }
                hour = String.valueOf(houri);
            }
            hour = hour + " ";

            String minute = String.format("%02d",mCalendar.get(Calendar.MINUTE));

            String second = String.format("%02d",mCalendar.get(Calendar.SECOND));

            String day = (mDayOfWeekFormat.format(mDate));

            String date = (mDateFormat.format(mDate));
            canvas.drawBitmap(mBackgroundBitmap, 0, 0, mBackgroundPaint);


            float texthourlen = mTextHourPaint.measureText(hour);
            float textminutelen = mTextMinutePaint.measureText(minute);
            float textsecondlen = mTextSecondPaint.measureText(second);
            float textdaylen = mTextSecondPaint.measureText(day);
            float textdatelen = mTextSecondPaint.measureText(date);

            Rect textbounds = new Rect();
            mTextHourPaint.getTextBounds(hour, 0, hour.length(), textbounds);
            int hourheight = textbounds.height();
            //int hourheight = 50;

            mYOffset = (canvas.getHeight() / 2) + (hourheight /2);
            int middleYOffset = (canvas.getHeight() / 2);
            mXOffset = (canvas.getWidth() /2) - ( (Math.round(texthourlen) + Math.round(textminutelen)) /2);

            int totaltimelen = Math.round(texthourlen) + Math.round(textminutelen);

            int dayxpos = (((canvas.getWidth() / 2) + (totaltimelen / 2) ) - Math.round(textdaylen)) - 20;
            dayxpos = (canvas.getWidth() / 2) - ( (Math.round(textdaylen) / 2 ) );


            canvas.drawText(day,dayxpos,middleYOffset-75, mTextDayPaint);

            canvas.drawText(hour, mXOffset, mYOffset, mTextHourPaint);
            canvas.drawText(minute, mXOffset+ Math.round(texthourlen), mYOffset, mTextMinutePaint);

            int secondxpos = (((canvas.getWidth() / 2) + (totaltimelen / 2) ) - Math.round(textsecondlen)) - 10;
            secondxpos = (canvas.getWidth() / 2) - ( Math.round(textsecondlen) /2 );


            if (!isInAmbientMode()) {

                //canvas.drawText(second, (canvas.getWidth() /2) - (Math.round(textsecondlen) / 2) , canvas.getHeight() - 45, mTextSecondPaint);
                canvas.drawText(second, secondxpos , middleYOffset+145, mTextSecondPaint);
            }

            int datexpos = (canvas.getWidth() / 2) - ( (Math.round(textdatelen) / 2 ) );
            //canvas.drawText(date,mXOffset+10,middleYOffset+95, mTextDatePaint);
            canvas.drawText(date,datexpos,middleYOffset+95, mTextDatePaint);




        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }
    }
}
