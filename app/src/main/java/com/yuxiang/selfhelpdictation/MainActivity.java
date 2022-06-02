package com.yuxiang.selfhelpdictation;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private long exitTime = 0;
    private long enterTile;

    private List<String> column;
    private String[] line;

    private boolean isReady;
    private boolean isRespond;
    private boolean isPlaySound;

    private File file;
    private String textTmp;

    private TextView title;
    private TextView content;
    private EditText text;
    private TextView answer;

    private SharedPreferences setting;
    private ClipboardManager cm;
    private ClipData clipData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        title = findViewById(R.id.title);
        content = findViewById(R.id.content);
        text = findViewById(R.id.text);
        answer = findViewById(R.id.answer);

        if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            answer.setPadding(0, (int) (5 * (getResources().getDisplayMetrics().density) + 0.5f), 0, (int) (5 * (getResources().getDisplayMetrics().density) + 0.5f));
        } else if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            answer.setPadding(0, (int) (5 * (getResources().getDisplayMetrics().density) + 0.5f), 0, (int) (215 * (getResources().getDisplayMetrics().density) + 0.5f));
        }

        setting = this.getSharedPreferences("setting", Context.MODE_PRIVATE);

        isRespond = false;
        isPlaySound = setting.getBoolean("is_play_sound", true);
        file = new File("/storage/emulated/0/Download/test.txt");
        isReady = (file.exists() && (PackageManager.PERMISSION_GRANTED == getPackageManager().checkPermission("android.permission.READ_EXTERNAL_STORAGE", "com.yuxiang.selfhelpdictation")));
        column = new ArrayList<>();

        enterTile = System.currentTimeMillis();
        cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

        text.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND || actionId == EditorInfo.IME_ACTION_DONE || (event != null) && KeyEvent.KEYCODE_ENTER == event.getKeyCode() && KeyEvent.ACTION_DOWN == event.getAction()) {
                if ((System.currentTimeMillis() - enterTile) > 200) {
                    enterTile = System.currentTimeMillis();
                    check();
                }
            }
            return true;
        });

        title.setOnClickListener(view -> check());

        answer.setOnClickListener(view -> {
            if (!"*".equals(answer.getText().toString())) {
                clipData = ClipData.newPlainText("answer", answer.getText().toString());
                cm.setPrimaryClip(clipData);
                Toast.makeText(this, "文本复制成功", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            answer.setPadding(0, (int) (5 * (getResources().getDisplayMetrics().density) + 0.5f), 0, (int) (5 * (getResources().getDisplayMetrics().density) + 0.5f));
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            answer.setPadding(0, (int) (5 * (getResources().getDisplayMetrics().density) + 0.5f), 0, (int) (215 * (getResources().getDisplayMetrics().density) + 0.5f));
        }
    }

    private void check() {
        if (isReady) {
            textTmp = text.getText().toString();
            if (column.size() == 0) {
                readFile();
            }
            if (isRespond) {
                judge();
            } else {
                respond();
            }
        } else {
            Toast.makeText(MainActivity.this, "没有存储权限或文件不存在", Toast.LENGTH_SHORT).show();
        }
    }

    private void respond() {
        text.setText("");
        title.setText("请回答");
        line = column.get((int) (Math.random() * column.size())).split(",");
        content.setText(line[0]);
        answer.setText("*");
        isRespond = true;
    }

    private void judge() {
        if (textTmp.equals(line[1])) {
            title.setText("回答正确");
        } else if ("".equals(textTmp)) {
            title.setText("未作答");
        } else {
            title.setText("回答错误");
        }
        answer.setText(line[1]);
        isRespond = !textTmp.equals(line[1]);
        if (isPlaySound) {
            try {
                playMusic("https://fanyi.sogou.com/reventondc/synthesis?text=" + URLEncoder.encode(line[1], "utf-8") + "&speed=1&lang=zh-CHS&from=translateweb&speaker=6");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    }

    private void readFile() {
        FileReader fr = null;
        BufferedReader br = null;
        try {
            fr = new FileReader(file);
            br = new BufferedReader(fr);
            String tmp;
            while ((tmp = br.readLine()) != null) {
                column.add(tmp);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fr != null) {
                try {
                    fr.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void playMusic(String url) {
        MediaPlayer mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(MediaPlayer::start);
            mediaPlayer.setOnCompletionListener(mediaPlayer12 -> {
                mediaPlayer12.stop();
                mediaPlayer12.release();
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            if (column.size() != 0) {
                text.setText(line[1]);
            } else {
                isPlaySound = !isPlaySound;
                SharedPreferences.Editor editor = setting.edit();
                editor.putBoolean("is_play_sound", isPlaySound);
                editor.apply();
                if (isPlaySound) {
                    Toast.makeText(this, "语音播报已开启", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "语音播报已关闭", Toast.LENGTH_SHORT).show();
                }
            }
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            text.setText("");
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            exit();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void exit() {
        if ((System.currentTimeMillis() - exitTime) > 2000) {
            Toast.makeText(MainActivity.this, "再按一次退出程序", Toast.LENGTH_SHORT).show();
            exitTime = System.currentTimeMillis();
        } else {
            finish();
            System.exit(0);
        }
    }
}
