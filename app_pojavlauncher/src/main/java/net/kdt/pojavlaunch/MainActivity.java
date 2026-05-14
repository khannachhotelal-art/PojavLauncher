package net.kdt.koraxlaunch;

import static net.kdt.KORAXLaunch.Tools.currentDisplayMetrics;
import static net.kdt.KORAXLaunch.Tools.dialogForceClose;
import static net.kdt.KORAXLaunch.prefs.LauncherPreferences.PREF_ENABLE_GYRO;
import static net.kdt.KORAXLaunch.prefs.LauncherPreferences.PREF_SUSTAINED_PERFORMANCE;
import static net.kdt.KORAXLaunch.prefs.LauncherPreferences.PREF_USE_ALTERNATE_SURFACE;
import static net.kdt.KORAXLaunch.prefs.LauncherPreferences.PREF_VIRTUAL_MOUSE_START;
import static org.lwjgl.glfw.CallbackBridge.sendKeyPress;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.*;
import android.util.Log;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.kdt.LoggerView;

import net.kdt.KORAXLaunch.customcontrols.*;
import net.kdt.KORAXLaunch.customcontrols.keyboard.*;
import net.kdt.KORAXLaunch.customcontrols.mouse.*;
import net.kdt.KORAXLaunch.lifecycle.ContextExecutor;
import net.kdt.KORAXLaunch.prefs.*;
import net.kdt.KORAXLaunch.services.GameService;
import net.kdt.KORAXLaunch.utils.*;
import net.kdt.KORAXLaunch.value.*;
import net.kdt.KORAXLaunch.value.launcherprofiles.*;

import org.lwjgl.glfw.CallbackBridge;

import java.io.File;
import java.io.IOException;

public class MainActivity extends BaseActivity implements ControlButtonMenuListener, EditorExitable, ServiceConnection {

    public static volatile ClipboardManager GLOBAL_CLIPBOARD;
    public static final String INTENT_MINECRAFT_VERSION = "intent_version";

    public static TouchCharInput touchCharInput;
    private MinecraftGLSurface minecraftGLView;
    private static Touchpad touchpad;
    private LoggerView loggerView;
    private DrawerLayout drawerLayout;
    private ListView navDrawer;
    private View mDrawerPullButton;
    private GyroControl mGyroControl;
    private ControlLayout mControlLayout;
    private HotbarView mHotbarView;

    private boolean isInEditor;
    private QuickSettingSideDialog mQuickSettingSideDialog;
    private GameService.LocalBinder mServiceBinder;

    MinecraftProfile minecraftProfile;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        minecraftProfile = LauncherProfiles.getCurrentProfile();

        Intent intent = new Intent(this, GameService.class);
        ContextCompat.startForegroundService(this, intent);

        initLayout(R.layout.activity_basemain);

        // 💚 LIME THEME FIX (STATUS + NAV BAR)
        getWindow().setStatusBarColor(Color.parseColor("#32CD32"));
        getWindow().setNavigationBarColor(Color.parseColor("#228B22"));

        if (PREF_USE_ALTERNATE_SURFACE)
            getWindow().setBackgroundDrawable(null);
        else
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#101010")));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            getWindow().setSustainedPerformanceMode(PREF_SUSTAINED_PERFORMANCE);

        // 🌿 GLOBAL UI IMPROVEMENT (anti-blue feel)
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );

        mGyroControl = new GyroControl(this);

        ContextExecutor.setActivity(this);
        bindService(intent, this, 0);
    }

    protected void initLayout(int resId) {
        setContentView(resId);
        bindValues();

        mControlLayout.setMenuListener(this);

        // 💚 button highlight fallback (replaces default blue tint feel)
        mDrawerPullButton.setBackgroundColor(Color.parseColor("#32CD32"));

        mDrawerPullButton.setOnClickListener(v -> onClickedMenu());
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

        try {
            GLOBAL_CLIPBOARD = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            touchCharInput.setCharacterSender(new LwjglCharSender());

            setTitle("Minecraft " + minecraftProfile.lastVersionId);

            String version = getIntent().getStringExtra(INTENT_MINECRAFT_VERSION);
            if (version == null) version = minecraftProfile.lastVersionId;

            JMinecraftVersionList.Version info = Tools.getVersionInfo(version);

            Tools.getDisplayMetrics(this);

            final String finalVersion = version;

            minecraftGLView.setSurfaceReadyListener(() -> {
                if (PREF_VIRTUAL_MOUSE_START)
                    touchpad.post(() -> touchpad.switchState());

                runCraft(finalVersion, info);
            });

        } catch (Throwable e) {
            Tools.showError(this, e, true);
        }
    }

    // 🎮 Play system unchanged but safe
    private void runCraft(String versionId, JMinecraftVersionList.Version version) throws Throwable {
        MinecraftAccount acc = PojavProfile.getCurrentProfileContent(this, null);

        Tools.launchMinecraft(this, acc, minecraftProfile, versionId,
                version.javaVersion != null ? version.javaVersion.majorVersion : 8);

        Tools.runOnUiThread(() -> mServiceBinder.isActive = false);
    }

    private void bindValues() {
        mControlLayout = findViewById(R.id.main_control_layout);
        minecraftGLView = findViewById(R.id.main_game_render_view);
        touchpad = findViewById(R.id.main_touchpad);
        drawerLayout = findViewById(R.id.main_drawer_options);
        navDrawer = findViewById(R.id.main_navigation_view);
        loggerView = findViewById(R.id.mainLoggerView);
        touchCharInput = findViewById(R.id.mainTouchCharInput);
        mDrawerPullButton = findViewById(R.id.drawer_button);
        mHotbarView = findViewById(R.id.hotbar_view);
    }

    // 🎯 Quick UI polish functions
    private void applyLimeAccent(View v) {
        if (v != null) v.setBackgroundColor(Color.parseColor("#32CD32"));
    }
    }
