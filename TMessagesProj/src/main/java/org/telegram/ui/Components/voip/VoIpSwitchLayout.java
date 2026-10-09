package org.telegram.ui.Components.voip;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import android.view.animation.OvershootInterpolator;
import org.telegram.ui.Components.ButtonBounce;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RLottieDrawable;

@SuppressLint("ViewConstructor")
public class VoIpSwitchLayout extends FrameLayout {

    public enum Type {
        MICRO,
        CAMERA,
        VIDEO,
        BLUETOOTH,
        SPEAKER,
    }

    private final VoIPBackgroundProvider backgroundProvider;
    private VoIpButtonView voIpButtonView;
    private Type type;
    private final TextView currentTextView;
    private final TextView newTextView;
    public int animationDelay;

    public void setOnBtnClickedListener(VoIpButtonView.OnBtnClickedListener onBtnClickedListener) {
        voIpButtonView.setOnBtnClickedListener(onBtnClickedListener);
    }

    public VoIpSwitchLayout(@NonNull Context context, VoIPBackgroundProvider backgroundProvider) {
        super(context);
        this.backgroundProvider = backgroundProvider;
        setWillNotDraw(true);
        voIpButtonView = new VoIpButtonView(context, backgroundProvider);
        addView(voIpButtonView, LayoutHelper.createFrame(VoIpButtonView.ITEM_SIZE + 1.5f, VoIpButtonView.ITEM_SIZE + 1.5f, Gravity.CENTER_HORIZONTAL));

        currentTextView = new TextView(context);
        currentTextView.setGravity(Gravity.CENTER_HORIZONTAL);
        currentTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
        currentTextView.setTextColor(Color.WHITE);
        currentTextView.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        addView(currentTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, VoIpButtonView.ITEM_SIZE + 6, 0, 2));

        newTextView = new TextView(context);
        newTextView.setGravity(Gravity.CENTER_HORIZONTAL);
        newTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
        newTextView.setTextColor(Color.WHITE);
        newTextView.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        addView(newTextView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, VoIpButtonView.ITEM_SIZE + 6, 0, 2));
        currentTextView.setVisibility(GONE);
        newTextView.setVisibility(GONE);
    }

    private void setText(Type type, boolean isSelectedState) {
        final String newText;
        switch (type) {
            case MICRO:
                if (isSelectedState) {
                    newText = LocaleController.getString(R.string.VoipUnmute);
                } else {
                    newText = LocaleController.getString(R.string.VoipMute);
                }
                break;
            case CAMERA:
                newText = LocaleController.getString(R.string.VoipFlip);
                break;
            case VIDEO:
                if (isSelectedState) {
                    newText = LocaleController.getString(R.string.VoipStartVideo);
                } else {
                    newText = LocaleController.getString(R.string.VoipStopVideo);
                }
                break;
            case BLUETOOTH:
                newText = LocaleController.getString(R.string.VoipAudioRoutingBluetooth);
                break;
            case SPEAKER:
                newText = LocaleController.getString(R.string.VoipSpeaker);
                break;
            default:
                newText = "";
        }
        setContentDescription(newText);

        if (currentTextView.getVisibility() == GONE && newTextView.getVisibility() == GONE) {
            currentTextView.setVisibility(VISIBLE);
            currentTextView.setText(newText);
            newTextView.setText(newText);
            return;
        }

        if (newTextView.getText().equals(newText) && currentTextView.getText().equals(newText)) {
            return;
        }

        currentTextView.animate().alpha(0f).translationY(-AndroidUtilities.dp(4)).setDuration(140).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                currentTextView.setText(newText);
                currentTextView.setTranslationY(0);
                currentTextView.setAlpha(1.0f);
            }
        }).start();
        newTextView.setText(newText);
        newTextView.setVisibility(VISIBLE);
        newTextView.setAlpha(0);
        newTextView.setTranslationY(AndroidUtilities.dp(5));
        newTextView.animate().alpha(1.0f).translationY(0).setDuration(150).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                newTextView.setVisibility(GONE);
            }
        }).start();
    }

    private void attachNewButton(int rawRes, int size, boolean isSelected, Type type) {
        final VoIpButtonView newVoIpButtonView = new VoIpButtonView(getContext(), backgroundProvider);
        if (rawRes == R.raw.camera_flip2) {
            newVoIpButtonView.singleIcon = new RLottieDrawable(rawRes, size, size, true, null);
            newVoIpButtonView.singleIcon.setMasterParent(newVoIpButtonView);
        } else {
            newVoIpButtonView.unSelectedIcon = new RLottieDrawable(rawRes, size, size, true, null);
            newVoIpButtonView.selectedIcon = new RLottieDrawable(rawRes, size, size, true, null);
            newVoIpButtonView.selectedIcon.setColorFilter(new PorterDuffColorFilter(Color.BLACK, PorterDuff.Mode.MULTIPLY));
        }
        newVoIpButtonView.setSelectedState(isSelected, false, type);
        newVoIpButtonView.setAlpha(0f);
        newVoIpButtonView.setOnBtnClickedListener(voIpButtonView.onBtnClickedListener);
        addView(newVoIpButtonView, LayoutHelper.createFrame(VoIpButtonView.ITEM_SIZE + 1.5f, VoIpButtonView.ITEM_SIZE + 1.5f, Gravity.CENTER_HORIZONTAL));
        final VoIpButtonView oldVoIpButton = voIpButtonView;
        voIpButtonView = newVoIpButtonView;
        newVoIpButtonView.animate().alpha(1f).setDuration(250).start();
        oldVoIpButton.animate().alpha(0f).setDuration(250).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                removeView(oldVoIpButton);
            }
        }).start();
    }

    public void setType(Type newType, boolean isSelected) {
        setType(newType, isSelected, false);
    }

    public void setType(Type newType, boolean isSelected, boolean fast) {
        if (this.type == newType && isSelected == voIpButtonView.isSelectedState) {
            if (getVisibility() != View.VISIBLE) {
                setVisibility(View.VISIBLE);
            }
            return;
        }
        if (getVisibility() != View.VISIBLE) {
            setVisibility(View.VISIBLE);
        }
        int size = AndroidUtilities.dp(VoIpButtonView.ITEM_SIZE + 1.5f);
        boolean ignoreSetState = false;
        switch (newType) {
            case MICRO:
                if (this.type != Type.MICRO) {
                    voIpButtonView.unSelectedIcon = new RLottieDrawable(R.raw.call_mute, size, size, true, null);
                    voIpButtonView.selectedIcon = new RLottieDrawable(R.raw.call_mute, size, size, true, null);
                    voIpButtonView.selectedIcon.setColorFilter(new PorterDuffColorFilter(Color.BLACK, PorterDuff.Mode.MULTIPLY));
                    voIpButtonView.selectedIcon.setMasterParent(voIpButtonView);
                }
                break;
            case VIDEO:
                //R.drawable.calls_sharescreen screencast is not used in the design
                if (this.type != Type.VIDEO) {
                    voIpButtonView.unSelectedIcon = new RLottieDrawable(R.raw.video_stop, size, size, true, null);
                    voIpButtonView.selectedIcon = new RLottieDrawable(R.raw.video_stop, size, size, true, null);
                    voIpButtonView.selectedIcon.setColorFilter(new PorterDuffColorFilter(Color.BLACK, PorterDuff.Mode.MULTIPLY));
                    voIpButtonView.selectedIcon.setMasterParent(voIpButtonView);
                }
                break;
            case CAMERA:
                if (this.type == Type.SPEAKER || this.type == Type.BLUETOOTH) {
                    ignoreSetState = true;
                    attachNewButton(R.raw.camera_flip2, size, isSelected, newType);
                } else if (this.type != Type.CAMERA) {
                    voIpButtonView.singleIcon = new RLottieDrawable(R.raw.camera_flip2, size, size, true, null);
                    voIpButtonView.singleIcon.setMasterParent(voIpButtonView);
                }
                break;
            case SPEAKER:
                if (this.type == Type.BLUETOOTH) {
                    ignoreSetState = isSelected == voIpButtonView.isSelectedState;
                    RLottieDrawable icon = isSelected ? voIpButtonView.selectedIcon : voIpButtonView.unSelectedIcon;
                    icon.setMasterParent(voIpButtonView);
                    icon.setOnAnimationEndListener(() -> AndroidUtilities.runOnUIThread(() -> attachSpeakerToBt(size)));
                    icon.start();
                } else if (this.type == Type.CAMERA) {
                    ignoreSetState = true;
                    attachNewButton(R.raw.speaker_to_bt, size, isSelected, newType);
                } else if (this.type != Type.SPEAKER) {
                    attachSpeakerToBt(size);
                }
                break;
            case BLUETOOTH:
                if (this.type == Type.SPEAKER) {
                    ignoreSetState = isSelected == voIpButtonView.isSelectedState;
                    RLottieDrawable icon = isSelected ? voIpButtonView.selectedIcon : voIpButtonView.unSelectedIcon;
                    icon.setMasterParent(voIpButtonView);
                    icon.setOnAnimationEndListener(() -> AndroidUtilities.runOnUIThread(() -> attachBtToSpeaker(size)));
                    icon.start();
                } else if (this.type == Type.CAMERA) {
                    ignoreSetState = true;
                    attachNewButton(R.raw.bt_to_speaker, size, isSelected, newType);
                } else if (this.type != Type.BLUETOOTH) {
                    attachBtToSpeaker(size);
                }
                break;
        }

        if (!ignoreSetState) {
            voIpButtonView.setSelectedState(isSelected, this.type != null && !fast, newType);
        }
        setText(newType, isSelected);
        this.type = newType;
    }

    private void attachSpeakerToBt(int size) {
        voIpButtonView.unSelectedIcon = new RLottieDrawable(R.raw.speaker_to_bt, size, size, true, null);
        voIpButtonView.selectedIcon = new RLottieDrawable(R.raw.speaker_to_bt, size, size, true, null);
        voIpButtonView.selectedIcon.setColorFilter(new PorterDuffColorFilter(Color.BLACK, PorterDuff.Mode.MULTIPLY));
    }

    private void attachBtToSpeaker(int size) {
        voIpButtonView.unSelectedIcon = new RLottieDrawable(R.raw.bt_to_speaker, size, size, true, null);
        voIpButtonView.selectedIcon = new RLottieDrawable(R.raw.bt_to_speaker, size, size, true, null);
        voIpButtonView.selectedIcon.setColorFilter(new PorterDuffColorFilter(Color.BLACK, PorterDuff.Mode.MULTIPLY));
    }

    public static class VoIpButtonView extends View {
        private static final int ITEM_SIZE = 52;

        private RLottieDrawable unSelectedIcon;
        private RLottieDrawable selectedIcon;
        private RLottieDrawable singleIcon;
        private final Paint maskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint whiteCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint darkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path clipPath = new Path();
        private final RectF rectF = new RectF();
        private final int maxRadius = AndroidUtilities.dp(ITEM_SIZE / 2f);
        private int unselectedRadius = maxRadius;
        private int selectedRadius = 0;
        private boolean isSelectedState = false;
        private int singleIconBackgroundAlphaPercent = 0;
        private OnBtnClickedListener onBtnClickedListener;
        private ValueAnimator animator;
        private ValueAnimator morphAnimator;
        private float morphProgress = 0f;
        private final ButtonBounce buttonBounce = new ButtonBounce(this, 1f, 3.5f);
        private final VoIPBackgroundProvider backgroundProvider;

        public void setSelectedState(boolean selectedState, boolean animate, Type type) {
            if (morphAnimator != null) {
                morphAnimator.removeAllUpdateListeners();
                morphAnimator.cancel();
                morphAnimator = null;
            }
            if (animator != null && animator.isRunning()) {
                animator.removeAllUpdateListeners();
                animator.cancel();
                animator = null;
                animate = false;
            }
            if (animate) {
                float targetMorph = selectedState ? 1f : 0f;
                morphAnimator = ValueAnimator.ofFloat(morphProgress, targetMorph);
                morphAnimator.addUpdateListener(animation -> {
                    morphProgress = (float) animation.getAnimatedValue();
                    invalidate();
                });
                if (selectedState) {
                    morphAnimator.setInterpolator(new OvershootInterpolator(1.6f));
                    morphAnimator.setDuration(320);
                    if (selectedIcon != null) {
                        selectedIcon.setCurrentFrame(0, false);
                        selectedIcon.start();
                    }
                } else {
                    morphAnimator.setInterpolator(CubicBezierInterpolator.EASE_OUT_QUINT);
                    morphAnimator.setDuration(280);
                }
                morphAnimator.start();

                if (singleIcon != null) {
                    animator = selectedState ? ValueAnimator.ofInt(20, 100) : ValueAnimator.ofInt(100, 20);
                    animator.addUpdateListener(animation -> {
                        singleIconBackgroundAlphaPercent = (int) animation.getAnimatedValue();
                        invalidate();
                    });
                    animator.setDuration(280);
                    animator.start();
                    if (type == Type.CAMERA) {
                        singleIcon.setCurrentFrame(0, false);
                        singleIcon.start();
                    }
                } else {
                    animator = ValueAnimator.ofInt(0, maxRadius);
                    if (selectedState) {
                        unselectedRadius = maxRadius;
                        animator.addUpdateListener(animation -> {
                            selectedRadius = (int) animation.getAnimatedValue();
                            invalidate();
                        });
                        animator.addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                unselectedRadius = 0;
                                invalidate();
                            }
                        });
                        animator.setDuration(280);
                        animator.start();
                    } else {
                        selectedRadius = maxRadius;
                        animator.addUpdateListener(animation -> {
                            unselectedRadius = (int) animation.getAnimatedValue();
                            invalidate();
                        });
                        animator.addListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationEnd(Animator animation) {
                                selectedRadius = 0;
                                invalidate();
                            }
                        });
                        animator.setDuration(280);
                        animator.start();
                    }
                }
            } else {
                morphProgress = selectedState ? 1f : 0f;
                if (singleIcon != null) {
                    singleIconBackgroundAlphaPercent = selectedState ? 100 : 20;
                } else {
                    if (selectedState) {
                        selectedRadius = maxRadius;
                        unselectedRadius = 0;
                        singleIconBackgroundAlphaPercent = 100;
                        if (type == Type.VIDEO || type == Type.MICRO) {
                            selectedIcon.setCurrentFrame(selectedIcon.getFramesCount() - 1, false);
                        }
                    } else {
                        selectedRadius = 0;
                        unselectedRadius = maxRadius;
                        singleIconBackgroundAlphaPercent = 20;
                    }
                }
            }
            isSelectedState = selectedState;
            invalidate();
        }

        public interface OnBtnClickedListener {
            void onClicked(View view);
        }

        public void setOnBtnClickedListener(OnBtnClickedListener onBtnClickedListener) {
            this.onBtnClickedListener = onBtnClickedListener;
        }

        public VoIpButtonView(@NonNull Context context, VoIPBackgroundProvider backgroundProvider) {
            super(context);
            this.backgroundProvider = backgroundProvider;
            backgroundProvider.attach(this);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            whiteCirclePaint.setColor(Color.WHITE);

            maskPaint.setColor(Color.BLACK);
            maskPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));

            darkPaint.setColor(Color.BLACK);
            darkPaint.setColorFilter(new PorterDuffColorFilter(Color.BLACK, PorterDuff.Mode.SRC_ATOP));
            darkPaint.setAlpha(VoIPBackgroundProvider.DARK_LIGHT_DEFAULT_ALPHA);
        }

        private void setPressedBtn(boolean pressed) {
            buttonBounce.setPressed(pressed);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            canvas.save();
            float bounceScale = buttonBounce.getScale(0.12f);
            canvas.scale(bounceScale, bounceScale, getMeasuredWidth() / 2f, getMeasuredHeight() / 2f);
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;

            float left = getX() + ((View) getParent()).getX();
            float top = getY() + ((View) ((View) getParent()).getParent()).getY();
            backgroundProvider.setLightTranslation(left, top);

            rectF.set(cx - maxRadius, cy - maxRadius, cx + maxRadius, cy + maxRadius);
            float currentCornerRadius = AndroidUtilities.lerp(maxRadius, AndroidUtilities.dp(16), Math.max(0f, Math.min(1.15f, morphProgress)));

            if (singleIcon != null) {
                float p = Math.max((singleIconBackgroundAlphaPercent - 20) / 80f, morphProgress);
                float singleCorner = AndroidUtilities.lerp(maxRadius, AndroidUtilities.dp(16), Math.max(0f, Math.min(1.15f, p)));
                if (singleIconBackgroundAlphaPercent > 20 || morphProgress > 0f) {
                    darkPaint.setAlpha((int) (VoIPBackgroundProvider.DARK_LIGHT_DEFAULT_ALPHA * p));
                    whiteCirclePaint.setAlpha((int) (255 * p));
                    canvas.drawRoundRect(rectF, singleCorner, singleCorner, whiteCirclePaint);
                    singleIcon.draw(canvas, maskPaint);
                    singleIcon.draw(canvas, darkPaint);
                } else {
                    canvas.drawRoundRect(rectF, singleCorner, singleCorner, backgroundProvider.getLightPaint());
                    if (backgroundProvider.isReveal()) {
                        canvas.drawRoundRect(rectF, singleCorner, singleCorner, backgroundProvider.getRevealPaint());
                    }
                    singleIcon.draw(canvas);
                }
                canvas.restore();
                return;
            }
            if (selectedIcon == null || unSelectedIcon == null) {
                canvas.restore();
                return;
            }

            if (morphProgress <= 0f) {
                canvas.drawRoundRect(rectF, maxRadius, maxRadius, backgroundProvider.getLightPaint());
                if (backgroundProvider.isReveal()) {
                    canvas.drawRoundRect(rectF, maxRadius, maxRadius, backgroundProvider.getRevealPaint());
                }
                unSelectedIcon.setAlpha(255);
                unSelectedIcon.draw(canvas);
            } else if (morphProgress >= 1f) {
                canvas.drawRoundRect(rectF, AndroidUtilities.dp(16), AndroidUtilities.dp(16), whiteCirclePaint);
                selectedIcon.setAlpha(255);
                selectedIcon.draw(canvas, maskPaint);
                selectedIcon.setAlpha((int) (255 * VoIPBackgroundProvider.DARK_LIGHT_PERCENT));
                selectedIcon.draw(canvas);
            } else {
                canvas.drawRoundRect(rectF, currentCornerRadius, currentCornerRadius, backgroundProvider.getLightPaint());
                if (backgroundProvider.isReveal()) {
                    canvas.drawRoundRect(rectF, currentCornerRadius, currentCornerRadius, backgroundProvider.getRevealPaint());
                }

                int whiteAlpha = (int) (255 * Math.min(1f, morphProgress));
                if (whiteAlpha > 0) {
                    whiteCirclePaint.setAlpha(whiteAlpha);
                    canvas.drawRoundRect(rectF, currentCornerRadius, currentCornerRadius, whiteCirclePaint);
                }

                int unselectedAlpha = (int) (255 * (1f - Math.min(1f, morphProgress)));
                if (unselectedAlpha > 0) {
                    unSelectedIcon.setAlpha(unselectedAlpha);
                    unSelectedIcon.draw(canvas);
                }

                if (whiteAlpha > 0) {
                    selectedIcon.setAlpha(whiteAlpha);
                    selectedIcon.draw(canvas, maskPaint);
                    selectedIcon.setAlpha((int) (whiteAlpha * VoIPBackgroundProvider.DARK_LIGHT_PERCENT));
                    selectedIcon.draw(canvas);
                }
            }
            canvas.restore();
        }

        private boolean isAnimating() {
            return false;
        }

        private float startX;
        private float startY;

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouchEvent(MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    setPressedBtn(true);
                    startX = event.getX();
                    startY = event.getY();
                    break;
                case MotionEvent.ACTION_UP:
                    setPressedBtn(false);
                    float endX = event.getX();
                    float endY = event.getY();
                    if (isClick(startX, endX, startY, endY) && !isAnimating()) {
                        if (onBtnClickedListener != null) onBtnClickedListener.onClicked(this);
                    }
                    break;
                case MotionEvent.ACTION_CANCEL:
                    setPressedBtn(false);
                    break;
            }
            return true;
        }

        private boolean isClick(float startX, float endX, float startY, float endY) {
            float differenceX = Math.abs(startX - endX);
            float differenceY = Math.abs(startY - endY);
            return !(differenceX > AndroidUtilities.dp(48) || differenceY > AndroidUtilities.dp(48));
        }
    }
}
