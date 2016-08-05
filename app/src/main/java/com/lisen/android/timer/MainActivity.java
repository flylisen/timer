package com.lisen.android.timer;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    /**
     * 用于保存程序退出时的剩余时间，便于下次进入继续
     */
    private int mLeftTime;
    /**
     * 用于保存设置的默认时间
     */
    private int mStoreSetTime;
    /**
     * 自定义的倒计时view类
     */
    private TimerView myView;
    private SharedPreferences sharedPreferences;
    /**
     * 是否正在倒计时，用于说明按钮的状态
     */
    private boolean stateRunning = false;
    /**
     * 开始和暂停按钮
     */
    private Button button;
    private Toolbar mToolBar;
    /**
     * 任务开关，当为true表示任务取消
     */
    private boolean mTaskSwitch = false;
    /**
     * 转动的角度 0- 360 增量设为5
     */
    private float mAngle = 0.f;
    /**
     * 0 - 9 由于定时任务定时0.1秒，添加此变量定时1秒
     */
    private int mCount = 0;
    /**
     * 与定时任务关联的handler
     */
    Handler handler = new Handler();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        myView = (TimerView) findViewById(R.id.myView);
        /**
         * 设置倒计时完成回调接口
         */
        myView.setOnTaskFinishListener(new TimerView.OnTaskFinishListener() {
            @Override
            public void onTaskFinish() {
                //取消定时任务
                 mTaskSwitch = true;
                //调用系统闹钟
                ringNotify();
                //弹出确认对话框
                dialogSure();

            }
        });
        sharedPreferences = getPreferences(MODE_PRIVATE);
        button = (Button) findViewById(R.id.bt_main_activity);
        mToolBar = (Toolbar) findViewById(R.id.tool_bar_main_activity);
        if (mToolBar != null) {
            mToolBar.setTitle(getResources().getString(R.string.title_main_activity));
            mToolBar.inflateMenu(R.menu.main_menu);
            mToolBar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.menu_set_default_time:
                            inputTimeDialog();
                            break;
                        case R.id.menu_reset_time:
                            //重置按钮，取消任务
                            handler.removeCallbacks(task);
                            restore(mStoreSetTime);
                            break;
                        default:
                            break;
                    }

                    return true;
                }
            });
            mToolBar.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
        }
        if (button != null) {
            button.setOnClickListener(this);
        }
        // 取出未完成的时间
        mLeftTime = sharedPreferences.getInt("storeLeftTime", 60 * 60);
        // 取出设置的默认时间
        mStoreSetTime = sharedPreferences.getInt("storeSetTime", 60 * 60);
        restore(mLeftTime);
    }

    // 重置状态
    private void restore(int leftTime) {
        stateRunning = false;
        mTaskSwitch = false;
        mAngle = 0.f;
        mCount = 0;
        button.setText(getResources().getString(R.string.start));
        myView.reSetView(leftTime);
    }

    /**
     * 点击时间到对话框中的确定按钮时进行的操作
     */
    private void dialogSure() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(R.string.click_for_stop_ring)
                .setPositiveButton(R.string.sure, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mPlayer != null) {
                            mPlayer.stop();
                            mPlayer.release();
                        }
                        restore(mStoreSetTime);

                    }
                })
                .create()
                .show();
    }

    private  MediaPlayer mPlayer;

    /**
     * 调用系统铃声
     */
    private void ringNotify() {
         mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(MainActivity.this, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM));
            mPlayer.prepare();
            mPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 输入时间对话框
     */
    private void inputTimeDialog() {
        final EditText editText = new EditText(MainActivity.this);
        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle(R.string.set_default_time)
                .setView(editText)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //do nothing
                    }
                })
                .setPositiveButton(R.string.sure, null);
        final AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String inputText = editText.getText().toString();
                if (inputText.length() == 0) {
                    Toast.makeText(MainActivity.this, R.string.please_input_time, Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!isNumber(inputText)) {
                    Toast.makeText(MainActivity.this, R.string.please_input_integer, Toast.LENGTH_SHORT).show();
                    return;
                }

                int resetTime = Integer.valueOf(inputText) * 3600;
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt("storeSetTime", resetTime);
                editor.apply();
                mStoreSetTime = resetTime;
                handler.removeCallbacks(task);
                restore(resetTime);
                dialog.dismiss();
            }
        });


    }

    /**
     * 判断是否输入数字
     *
     * @param inputText
     * @return
     */

    private boolean isNumber(String inputText) {
        for (int i = inputText.length(); --i >= 0; ) {
            if (!Character.isDigit(inputText.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 定时任务
     */
    Runnable task = new Runnable() {
        @Override
        public void run() {
          //  mLeftTime--;
            mAngle = mAngle % 360;
            mCount = mCount % 10;
            myView.updateView(mAngle, mCount);
            mAngle += 5.0f;
            mCount++;
           // Log.d("TAG", "mAngle:" + mAngle);
            handler.postDelayed(task, 100 ); // 定时0.1秒
            if (mTaskSwitch) {
                handler.removeCallbacks(task);

            }
        }
    };

    /**
     * activity被销毁时保存时间
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mTaskSwitch = true;
        handler.removeCallbacks(task);
        mLeftTime = myView.getLeftTime();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("storeLeftTime", mLeftTime);
        editor.apply();
    }

    /**
     * 按钮点击事件
     * @param v
     */
    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.bt_main_activity) {
           button.setText((stateRunning ? R.string.start : R.string.pause));
            if (stateRunning) {
                handler.removeCallbacks(task);
                myView.setHintText(getResources().getString(R.string.pause));
            } else {
                handler.post(task);
                myView.setHintText(getResources().getString(R.string.counting));
            }
            stateRunning = !stateRunning;
        }
    }
}
