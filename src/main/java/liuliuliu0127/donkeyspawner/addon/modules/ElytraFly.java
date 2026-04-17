package liuliuliu0127.donkeyspawner.addon.modules;

import liuliuliu0127.donkeyspawner.addon.DonkeySpawnerAddon;
import liuliuliu0127.donkeyspawner.addon.events.TravelEvent;
//import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import liuliuliu0127.donkeyspawner.addon.utils.Timer;
//import anticope.rejects.utils.SeijaUtil.LagBackDetectUtil;
import liuliuliu0127.donkeyspawner.addon.utils.MathUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.lang.reflect.Field;
import java.lang.Integer;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.ChatFormatting;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.ClientInput;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Input;

import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.network.chat.Component;

//import meteordevelopment.meteorclient.events.packets.PacketEvent;
//import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
//import java.lang.reflect.Field;


public class ElytraFly extends Module {

    SettingGroup sgAutoTakeOff;

    private final Setting<Boolean> autoStart;
    //private final Setting<Boolean> packetStart;
    private final Setting<Double> startY;
    //private final Setting<Double> tryTakeoffDelay;

    SettingGroup sgmoveImprovement;

    private final Setting<Boolean> normalizeSpeed;
    private final Setting<Boolean> normalizeHorizontalOnly;
    private final Setting<Boolean> smoothVelocity;
    private final Setting<Double> smoothFactor;

    SettingGroup sgRiseMethod;

    private final Setting<Boolean> AlienV4RiseMethod;

    SettingGroup sgSpeed;

    private final Setting<Double> speed;
    
    //精细化水平速度设置
    private final Setting<Boolean> detailedHorizontalSpeed;
    private final Setting<Double> speedForward;
    private final Setting<Double> speedBack;
    private final Setting<Double> speedLeft;
    private final Setting<Double> speedRight;
    private final Setting<Double> speedForwardLeft;
    private final Setting<Double> speedForwardRight;
    private final Setting<Double> speedBackLeft;
    private final Setting<Double> speedBackRight;
    // 带 Shift 的水平速度
    private final Setting<Double> speedForwardShift;
    private final Setting<Double> speedBackShift;
    private final Setting<Double> speedLeftShift;
    private final Setting<Double> speedRightShift;
    // 带 Jump 的水平速度
    private final Setting<Double> speedForwardJump;
    private final Setting<Double> speedBackJump;
    private final Setting<Double> speedLeftJump;
    private final Setting<Double> speedRightJump;
    private final Setting<Boolean> syncDetailedSpeeds;


    private final Setting<Double> GlideSpeed;
    private final Setting<Double> DownSpeed;
    private final Setting<Double> accelerateSpeed;
    private final Setting<Double> upPitch;

    SettingGroup sgWaterSpeed;

    // --- 水中水平速度控制 ---
    private final Setting<Double> waterSpeed;
    private final Setting<Boolean> detailedHorizontalSpeedWater;
    private final Setting<Double> speedForwardWater;
    private final Setting<Double> speedBackWater;
    private final Setting<Double> speedLeftWater;
    private final Setting<Double> speedRightWater;
    private final Setting<Double> speedForwardLeftWater;
    private final Setting<Double> speedForwardRightWater;
    private final Setting<Double> speedBackLeftWater;
    private final Setting<Double> speedBackRightWater;
    // 水中带 Shift 的水平速度
    private final Setting<Double> speedForwardShiftWater;
    private final Setting<Double> speedBackShiftWater;
    private final Setting<Double> speedLeftShiftWater;
    private final Setting<Double> speedRightShiftWater;
    private final Setting<Double> speedForwardJumpWater;
    private final Setting<Double> speedBackJumpWater;
    private final Setting<Double> speedLeftJumpWater;
    private final Setting<Double> speedRightJumpWater;
    private final Setting<Boolean> syncDetailedSpeedsWater;

    private final Setting<Double> waterPitch;
    private final Setting<Double> waterAccelerateSpeed;
    private final Setting<Double> waterYawSpeed;

    SettingGroup sgAngel;

    private final Setting<Double> yawSpeed;
    private final Setting<Boolean> pitchSpoof;
    private final Setting<Integer> RotationPriority;
    private final Setting<Boolean> pitchSpoofInWater;
    private final Setting<Double> fakePitch;

    private final SettingGroup sgAutoPlane;

    private final Setting<Boolean> autoPlane;
    private final Setting<Integer> autoPlaneY;
    private final Setting<String> destinationX;
    private final Setting<String> destinationZ;
    private final Setting<Boolean> toggleAutoPlane;
    private final Setting<Boolean> playerDodge;

    private final SettingGroup sgVtol;

    private final Setting<Boolean> verticalTakeoff;
    private final Setting<Double> fireWorkDelay;
    private final Setting<Double> verticalMultiple;
    private final Setting<Double> verticalMaxSpeed;
    private final Setting<Boolean> autoUse;
    private final Setting<Boolean> autoSwitch;
    private final Setting<Boolean> silentSwitch;
    private final Setting<Boolean> InventoryAutoSwitch;
    private final Setting<Boolean> noSuicide;

    SettingGroup sgMisc;
    // --- DEBUG 设置组 ---
    private final Setting<Boolean> debugMode;
    private final Setting<Boolean> debugOutput;
    private final Setting<Boolean> debugNeverModifyGravity;
    private final Setting<Boolean> debugDisableGravityLock;
    private final Setting<Double> debugCompensationStrength;
    private final Setting<Double> debugCompensationDamping;
    private final Setting<Boolean> debugEnableCompensation;
    private final Setting<Double> debugNormalizeHorizontalLimitFactor;
    private final Setting<Double> debugSmoothFactorOverride;
    private final Setting<Double> debugDoNormalFlyDragX;
    private final Setting<Double> debugDoNormalFlyDragY;
    private final Setting<Double> debugDoNormalFlyDragZ;
    private final Setting<Double> debugAlienV4RawUpMultiplier;
    private final Setting<Double> debugAlienV4UpSpeedMultiplier;

    private long lastAutoStartAttempt = 0;
    private static final long AUTO_START_COOLDOWN_MS = 500;

    
    private boolean fieldDumped = false;
    private Field pitchField = null;
    Timer startTimer;
    double playerLastY;
    final float NO_OPERATION = -999.0F;
    float pos;
    int fangx;
    float fakeYaw;
    float offsetYaw;
    Vec3 vec;
    Vec3 oVec;
    Timer useTimer;
    //boolean shouldHover;//悬停功能修复
    
    public ElytraFly() {
        super(DonkeySpawnerAddon.CATEGORY, "ElytraFly", "Makes Elytra Flight better.made by KijiSeija,Deepseek and liuliuliu0127");

        this.sgAutoTakeOff = this.settings.createGroup("AutoTakeoff");
        this.autoStart = this.sgAutoTakeOff.add(((BoolSetting.Builder) (new BoolSetting.Builder())
            .name("easyTakeoff"))
            .defaultValue(true)
            .build());
        this.startY = this.sgAutoTakeOff.add(new DoubleSetting.Builder()
            .name("Takeoff Fall Motion speed")
            .description("The fall motion speed for Easy Takeoff")
            .defaultValue(0.0D)
            .range(0.0D, 1.0D)
            .sliderRange(0.0D, 1.0D)
            .visible(this.autoStart::get)
            .build());
        this.sgmoveImprovement = this.settings.createGroup("MoveImprovement");
        this.normalizeSpeed = this.sgmoveImprovement.add(new BoolSetting.Builder()
            .name("Normalize Speed")
            .description("Limit total speed to the configured value to avoid anti-cheat detection.")
            .defaultValue(true)
            .build());
        this.normalizeHorizontalOnly = this.sgmoveImprovement.add(new BoolSetting.Builder()
            .name("Normalize Horizontal Only")
            .description("Only normalize horizontal speed components. If disabled, vertical speed is also normalized.")
            .defaultValue(false)
            .visible(this.normalizeSpeed::get)
            .build());
        this.smoothVelocity = this.sgmoveImprovement.add(new BoolSetting.Builder()
            .name("Smooth Velocity")
            .description("Smoothly interpolate velocity changes to avoid anti-cheat detection.")
            .defaultValue(false)
            .build());

        this.smoothFactor = this.sgmoveImprovement.add(new DoubleSetting.Builder()
            .name("Smooth Factor")
            .description("Interpolation factor per tick (0.0 = no change, 1.0 = instant).")
            .defaultValue(0.970D)
            .range(0.0D, 1.0D)
            .sliderRange(0.0D, 1.0D)
            .visible(this.smoothVelocity::get)
            .build());        
        
        this.sgRiseMethod =this.settings.createGroup("Rise Method");
        this.AlienV4RiseMethod = this.sgRiseMethod.add(new BoolSetting.Builder()
            .name("Alienv4 Rise")
            .description("rise method stole form alien v4")
            .defaultValue(false)
            .build());

        this.sgSpeed = this.settings.createGroup("Speed");
        this.speed = this.sgSpeed.add(((DoubleSetting.Builder) (new DoubleSetting.Builder())
            .name("Horizontal-Speed"))
            .range(0.0D, 10.0D)
            .sliderRange(0.0D, 10.0D)
            .defaultValue(2.950D)
            .build());
 
        this.detailedHorizontalSpeed = this.sgSpeed.add(new BoolSetting.Builder()
            .name("Detailed Horizontal speed")
            .description("Enable individual horizontal speed settings for each movement key combination.")
            .defaultValue(true)
            .build());
        //精细化速度控制设置，启用后可以针对不同的按键组合设置不同的速度，关闭后所有组合使用全局速度设置
        this.speedForward = this.sgSpeed.add(new DoubleSetting.Builder()
            .name("Forward Speed (W)")
            .defaultValue(this.speed.get())
            .range(0.0, 10.0)
            .sliderRange(0.0, 10.0)
            .visible(this.detailedHorizontalSpeed::get)
            .build());
        this.speedBack = this.sgSpeed.add(new DoubleSetting.Builder()
            .name("Backward Speed (S)")
            .defaultValue(this.speed.get())
            .range(0.0, 10.0)
            .sliderRange(0.0, 10.0)
            .visible(this.detailedHorizontalSpeed::get)
            .build());
        this.speedLeft = this.sgSpeed.add(new DoubleSetting.Builder()
            .name("Left Speed (A)")
            .defaultValue(this.speed.get())
            .range(0.0, 10.0)
            .sliderRange(0.0, 10.0)
            .visible(this.detailedHorizontalSpeed::get)
            .build());
        this.speedRight = this.sgSpeed.add(new DoubleSetting.Builder()
            .name("Right Speed (D)")
            .defaultValue(this.speed.get())
            .range(0.0, 10.0)
            .sliderRange(0.0, 10.0)
            .visible(this.detailedHorizontalSpeed::get)
            .build());
        this.speedForwardLeft = this.sgSpeed.add(new DoubleSetting.Builder()
            .name("Forward-Left Speed (W+A)")
            .defaultValue(this.speed.get())
            .range(0.0, 10.0)
            .sliderRange(0.0, 10.0)
            .visible(this.detailedHorizontalSpeed::get)
            .build());
        this.speedForwardRight = this.sgSpeed.add(new DoubleSetting.Builder()
            .name("Forward-Right Speed (W+D)")
            .defaultValue(this.speed.get())
            .range(0.0, 10.0)
            .sliderRange(0.0, 10.0)
            .visible(this.detailedHorizontalSpeed::get)
            .build());
        this.speedBackLeft = this.sgSpeed.add(new DoubleSetting.Builder()
            .name("Backward-Left Speed (S+A)")
            .defaultValue(this.speed.get())
            .range(0.0, 10.0)
            .sliderRange(0.0, 10.0)
            .visible(this.detailedHorizontalSpeed::get)
            .build());
        this.speedBackRight = this.sgSpeed.add(new DoubleSetting.Builder()
            .name("Backward-Right Speed (S+D)")
            .defaultValue(this.speed.get())
            .range(0.0, 10.0)
            .sliderRange(0.0, 10.0)
            .visible(this.detailedHorizontalSpeed::get)
            .build());

        // 带 Shift 的组合
        this.speedForwardShift = this.sgSpeed.add(new DoubleSetting.Builder()
            .name("Forward-Shift Speed (W+Shift)")
            .defaultValue(this.speed.get())
            .range(0.0, 10.0)
            .sliderRange(0.0, 10.0)
            .visible(this.detailedHorizontalSpeed::get)
            .build());
        this.speedBackShift = this.sgSpeed.add(new DoubleSetting.Builder()
            .name("Backward-Shift Speed (S+Shift)")
            .defaultValue(this.speed.get())
            .range(0.0, 10.0)
            .sliderRange(0.0, 10.0)
            .visible(this.detailedHorizontalSpeed::get)
            .build());
        this.speedLeftShift = this.sgSpeed.add(new DoubleSetting.Builder()
            .name("Left-Shift Speed (A+Shift)")
            .defaultValue(this.speed.get())
            .range(0.0, 10.0)
            .sliderRange(0.0, 10.0)
            .visible(this.detailedHorizontalSpeed::get)
            .build());
        this.speedRightShift = this.sgSpeed.add(new DoubleSetting.Builder()
            .name("Right-Shift Speed (D+Shift)")
            .defaultValue(this.speed.get())
            .range(0.0, 10.0)
            .sliderRange(0.0, 10.0)
            .visible(this.detailedHorizontalSpeed::get)
            .build());

        // 带 Jump 的组合
        this.speedForwardJump = this.sgSpeed.add(new DoubleSetting.Builder()
            .name("Forward-Jump Speed (W+Jump)")
            .defaultValue(this.speed.get())
            .range(0.0, 10.0)
            .sliderRange(0.0, 10.0)
            .visible(this.detailedHorizontalSpeed::get)
            .build());
        this.speedBackJump = this.sgSpeed.add(new DoubleSetting.Builder()
            .name("Backward-Jump Speed (S+Jump)")
            .defaultValue(this.speed.get())
            .range(0.0, 10.0)
            .sliderRange(0.0, 10.0)
            .visible(this.detailedHorizontalSpeed::get)
            .build());
        this.speedLeftJump = this.sgSpeed.add(new DoubleSetting.Builder()
            .name("Left-Jump Speed (A+Jump)")
            .defaultValue(this.speed.get())
            .range(0.0, 10.0)
            .sliderRange(0.0, 10.0)
            .visible(this.detailedHorizontalSpeed::get)
            .build());
        this.speedRightJump = this.sgSpeed.add(new DoubleSetting.Builder()
            .name("Right-Jump Speed (D+Jump)")
            .defaultValue(this.speed.get())
            .range(0.0, 10.0)
            .sliderRange(0.0, 10.0)
            .visible(this.detailedHorizontalSpeed::get)
            .build());
        
        this.syncDetailedSpeeds = this.sgSpeed.add(new BoolSetting.Builder()
            .name("Sync to Current Speed")
            .description("Click and reopen the Gui so see the changes.Copy the current global Horizontal-Speed to all detailed speed settings.")
            .defaultValue(false)
            .visible(this.detailedHorizontalSpeed::get)
            .build());

        this.GlideSpeed = this.sgSpeed.add(((DoubleSetting.Builder) (new DoubleSetting.Builder())
            .name("GlideSpeed"))
            .range(0.0D, 10.0D)
            .sliderRange(0.0D, 10.0D)
            .defaultValue(0.0D)
            .build());
        this.DownSpeed = this.sgSpeed.add(((DoubleSetting.Builder) (new DoubleSetting.Builder())
            .name("DownSpeed"))
            .range(0.0D, 10.0D)
            .sliderRange(0.0D, 10.0D)
            .defaultValue(1.82D)
            .build());
        this.accelerateSpeed = this.sgSpeed.add(((DoubleSetting.Builder) (new DoubleSetting.Builder())
            .name("Rise-MinSpeed"))
            .range(0.0D, 3.0D)
            .sliderRange(0.0D, 3.0D)
            .defaultValue(0.5D)
            .build());
        this.upPitch = this.sgSpeed.add(((DoubleSetting.Builder) (new DoubleSetting.Builder())
            .name("RisePitch"))
            .range(0.0D, 90.0D)
            .sliderRange(0.0D, 90.0D)
            .defaultValue(60.0D)
            .build());
        
        this.sgWaterSpeed = this.settings.createGroup("WaterSpeed");
        // 全局水中水平速度
        this.waterSpeed = this.sgWaterSpeed.add(new DoubleSetting.Builder()
            .name("Water Horizontal-Speed")
            .description("Horizontal speed when flying in water.")
            .defaultValue(2.426D)
            .range(0.0, 10.0)
            .sliderRange(0.0, 10.0)
            .build());

        // 水中详细水平速度开关
        this.detailedHorizontalSpeedWater = this.sgWaterSpeed.add(new BoolSetting.Builder()
            .name("Detailed Horizontal speed (Water)")
            .description("Enable individual horizontal speed settings for each movement key combination in water.")
            .defaultValue(false)
            .build());

        this.speedForwardWater = this.sgWaterSpeed.add(new DoubleSetting.Builder()
            .name("Forward Speed Water (W)")
            .defaultValue(this.waterSpeed.get())
            .range(0.0, 10.0)
            .sliderRange(0.0, 10.0)
            .visible(this.detailedHorizontalSpeedWater::get)
            .build());
        this.speedBackWater = this.sgWaterSpeed.add(new DoubleSetting.Builder()
            .name("Backward Speed Water (S)")
            .defaultValue(this.waterSpeed.get())
            .range(0.0, 10.0)
            .sliderRange(0.0, 10.0)
            .visible(this.detailedHorizontalSpeedWater::get)
            .build());
        this.speedLeftWater = this.sgWaterSpeed.add(new DoubleSetting.Builder()
            .name("Left Speed Water (A)")
            .defaultValue(this.waterSpeed.get())
            .range(0.0, 10.0)
            .sliderRange(0.0, 10.0)
            .visible(this.detailedHorizontalSpeedWater::get)
            .build());
        this.speedRightWater = this.sgWaterSpeed.add(new DoubleSetting.Builder()
            .name("Right Speed Water (D)")
            .defaultValue(this.waterSpeed.get())
            .range(0.0, 10.0)
            .sliderRange(0.0, 10.0)
            .visible(this.detailedHorizontalSpeedWater::get)
            .build());
        this.speedForwardLeftWater = this.sgWaterSpeed.add(new DoubleSetting.Builder()
            .name("Forward-Left Speed Water (W+A)")
            .defaultValue(this.waterSpeed.get())
            .range(0.0, 10.0)
            .sliderRange(0.0, 10.0)
            .visible(this.detailedHorizontalSpeedWater::get)
            .build());
        this.speedForwardRightWater = this.sgWaterSpeed.add(new DoubleSetting.Builder()
            .name("Forward-Right Speed Water (W+D)")
            .defaultValue(this.waterSpeed.get())
            .range(0.0, 10.0)
            .sliderRange(0.0, 10.0)
            .visible(this.detailedHorizontalSpeedWater::get)
            .build());
        this.speedBackLeftWater = this.sgWaterSpeed.add(new DoubleSetting.Builder()
            .name("Backward-Left Speed Water (S+A)")
            .defaultValue(this.waterSpeed.get())
            .range(0.0, 10.0)
            .sliderRange(0.0, 10.0)
            .visible(this.detailedHorizontalSpeedWater::get)
            .build());
        this.speedBackRightWater = this.sgWaterSpeed.add(new DoubleSetting.Builder()
            .name("Backward-Right Speed Water (S+D)")
            .defaultValue(this.waterSpeed.get())
            .range(0.0, 10.0)
            .sliderRange(0.0, 10.0)
            .visible(this.detailedHorizontalSpeedWater::get)
            .build());
        // 水中带 Shift 的组合
        this.speedForwardShiftWater = this.sgWaterSpeed.add(new DoubleSetting.Builder()
            .name("Forward-Shift Speed Water (W+Shift)")
            .defaultValue(this.waterSpeed.get())
            .range(0.0, 10.0)
            .sliderRange(0.0, 10.0)
            .visible(this.detailedHorizontalSpeedWater::get)
            .build());
        this.speedBackShiftWater = this.sgWaterSpeed.add(new DoubleSetting.Builder()
            .name("Backward-Shift Speed Water (S+Shift)")
            .defaultValue(this.waterSpeed.get())
            .range(0.0, 10.0)
            .sliderRange(0.0, 10.0)
            .visible(this.detailedHorizontalSpeedWater::get)
            .build());
        this.speedLeftShiftWater = this.sgWaterSpeed.add(new DoubleSetting.Builder()
            .name("Left-Shift Speed Water (A+Shift)")
            .defaultValue(this.waterSpeed.get())
            .range(0.0, 10.0)
            .sliderRange(0.0, 10.0)
            .visible(this.detailedHorizontalSpeedWater::get)
            .build());
        this.speedRightShiftWater = this.sgWaterSpeed.add(new DoubleSetting.Builder()
            .name("Right-Shift Speed Water (D+Shift)")
            .defaultValue(this.waterSpeed.get())
            .range(0.0, 10.0)
            .sliderRange(0.0, 10.0)
            .visible(this.detailedHorizontalSpeedWater::get)
            .build());
        // 水中带 Jump 的组合
        this.speedForwardJumpWater = this.sgWaterSpeed.add(new DoubleSetting.Builder()
            .name("Forward-Jump Speed Water (W+Jump)")
            .defaultValue(this.waterSpeed.get())
            .range(0.0, 10.0)
            .sliderRange(0.0, 10.0)
            .visible(this.detailedHorizontalSpeedWater::get)
            .build());
        this.speedBackJumpWater = this.sgWaterSpeed.add(new DoubleSetting.Builder()
            .name("Backward-Jump Speed Water (S+Jump)")
            .defaultValue(this.waterSpeed.get())
            .range(0.0, 10.0)
            .sliderRange(0.0, 10.0)
            .visible(this.detailedHorizontalSpeedWater::get)
            .build());
        this.speedLeftJumpWater = this.sgWaterSpeed.add(new DoubleSetting.Builder()
            .name("Left-Jump Speed Water (A+Jump)")
            .defaultValue(this.waterSpeed.get())
            .range(0.0, 10.0)
            .sliderRange(0.0, 10.0)
            .visible(this.detailedHorizontalSpeedWater::get)
            .build());
        this.speedRightJumpWater = this.sgWaterSpeed.add(new DoubleSetting.Builder()
            .name("Right-Jump Speed Water (D+Jump)")
            .defaultValue(this.waterSpeed.get())
            .range(0.0, 10.0)
            .sliderRange(0.0, 10.0)
            .visible(this.detailedHorizontalSpeedWater::get)
            .build());

        this.syncDetailedSpeedsWater = this.sgWaterSpeed.add(new BoolSetting.Builder()
            .name("Sync to Water Speed")
            .description("Click and reopen the Gui so see the changes.Copy the current global Water Horizontal-Speed to all detailed water speed settings.")
            .defaultValue(false)
            .visible(this.detailedHorizontalSpeedWater::get)
            .build());
        this.waterPitch = this.sgWaterSpeed.add(((DoubleSetting.Builder) (new DoubleSetting.Builder())
            .name("WaterRisePitch"))
            .range(0.0D, 90.0D)
            .sliderRange(0.0D, 90.0D)
            .defaultValue(45.0D)
            .build());
        this.waterAccelerateSpeed = this.sgWaterSpeed.add(((DoubleSetting.Builder) (new DoubleSetting.Builder())
            .name("WaterRiseMinSpeed"))
            .range(0.0D, 3.0D)
            .sliderRange(0.0D, 3.0D)
            .defaultValue(0.417D)
            .build());
        this.waterYawSpeed = this.sgWaterSpeed.add(((DoubleSetting.Builder) (new DoubleSetting.Builder())
            .name("WaterYawSpeedOffset"))
            .range(0.0D, 40.0D)
            .sliderRange(0.0D, 40.0D)
            .defaultValue(1.0D)
            .build());

        this.sgAngel = this.settings.createGroup("Angel");
        this.yawSpeed = this.sgAngel.add(((DoubleSetting.Builder) (new DoubleSetting.Builder())
            .name("Yaw-Velocity"))
            .range(-180.0D, 180.0D)
            .sliderRange(-180.0D, 180.0D)
            .defaultValue(51.5D)
            .build());
        this.pitchSpoof = this.sgAngel.add(((BoolSetting.Builder) ((BoolSetting.Builder) (new BoolSetting.Builder())
            .name("PitchSpoof"))
            .defaultValue(true))
            .build());
        this.pitchSpoofInWater = this.sgAngel.add(new BoolSetting.Builder()
            .name("PitchSpoof In Water")
            .description("Enable pitch spoofing while in water or lava.")
            .defaultValue(false)   // 默认关闭，保持水中原有行为
            .visible(this.pitchSpoof::get)
            .build());
        this.RotationPriority = this.sgAngel.add(new IntSetting.Builder()
            .name("[Debug]meteor rotation priority")
            .range(0, 100)
            .sliderRange(0, 100)
            .defaultValue(100)
            .build());
        this.fakePitch = this.sgAngel.add(new DoubleSetting.Builder()
            .name("FakePitch")
            .range(-90.0D, 90.0D)
            .sliderRange(-90.0D, 90.0D)
            .defaultValue(10.0D)
            .visible(this.pitchSpoof::get)
            .build());
        this.sgAutoPlane = this.settings.createGroup("AutoPlane");
        this.autoPlane = this.sgAutoPlane.add(((BoolSetting.Builder) ((BoolSetting.Builder) (new BoolSetting.Builder())
            .name("AutoPlane"))
            .defaultValue(false))
            .build());
        this.autoPlaneY = this.sgAutoPlane.add(new IntSetting.Builder()
            .name("autoPlaneY")
            .range(-1000, 4000)
            .sliderRange(0, 400)
            .defaultValue(257)
            .visible(this.autoPlane::get)
            .build());
        this.destinationX = this.sgAutoPlane.add(new StringSetting.Builder()
            .name("DestinationX")
            .defaultValue("0")
            .visible(this.autoPlane::get)
            .build());
        this.destinationZ = this.sgAutoPlane.add(new StringSetting.Builder()
            .name("DestinationZ")
            .defaultValue("0")
            .visible(this.autoPlane::get)
            .build());
        this.toggleAutoPlane = this.sgAutoPlane.add(new BoolSetting.Builder()
            .name("toggleAutoPlane")
            .defaultValue(true)
            .visible(this.autoPlane::get)
            .build());
        this.playerDodge = this.sgAutoPlane.add(new BoolSetting.Builder()
            .name("playerDodge")
            .defaultValue(false)
            .visible(this.autoPlane::get)
            .build());
        this.sgVtol = this.settings.createGroup("Vertical take-off");
        this.verticalTakeoff = this.sgVtol.add(((BoolSetting.Builder) ((BoolSetting.Builder) (new BoolSetting.Builder())
            .name("VerticalTakeoff"))
            .defaultValue(true))
            .build());
        this.fireWorkDelay = this.sgVtol.add(((DoubleSetting.Builder) (new DoubleSetting.Builder())
            .name("fireWorkDelay"))
            .defaultValue(2000)
            .sliderRange(0.0D, 5000.0D)
            .range(0.0D, 10000.0D)
            .build());
        this.verticalMultiple = this.sgVtol.add(((DoubleSetting.Builder) (new DoubleSetting.Builder())
            .name("VerticalMultiple"))
            .defaultValue(0.55D)
            .sliderRange(0.0D, 10.0D)
            .range(0.0D, 20.0D)
            .build());
        this.verticalMaxSpeed = this.sgVtol.add(((DoubleSetting.Builder) (new DoubleSetting.Builder())
            .name("VerticalMaxSpeed"))
            .defaultValue(1.8D)
            .sliderRange(0.0D, 10.0D)
            .range(0.0D, 20.0D)
            .build());
        this.autoUse = this.sgVtol.add(((BoolSetting.Builder) ((BoolSetting.Builder) (new BoolSetting.Builder())
            .name("AutoUseFirework"))
            .defaultValue(true))
            .build());
        this.autoSwitch = this.sgVtol.add(new BoolSetting.Builder()
            .name("AutoSwitchFirework")
            .defaultValue(true)
            .visible(this.autoUse::get)
            .build());
        this.silentSwitch = this.sgVtol.add(((BoolSetting.Builder) ((BoolSetting.Builder) ((BoolSetting.Builder) (new BoolSetting.Builder())
            .name("SilentSwitch"))
            .visible(() -> (this.autoSwitch.get() && this.autoUse.get())))
            .defaultValue(true))
            .build());
        this.InventoryAutoSwitch = this.sgVtol.add(new BoolSetting.Builder()
            .name("InventorySwitch")
            .description("Allow using fireworks in inventory when no fireworks in hotbar")
            .defaultValue(true)
            .visible(this.autoUse::get)
            .build());
        this.noSuicide = this.sgVtol.add(((BoolSetting.Builder) ((BoolSetting.Builder) ((BoolSetting.Builder) (new BoolSetting.Builder())
            .name("AntiSuicide"))
            .visible(() -> (this.autoSwitch.get() && this.autoUse.get())))
            .defaultValue(true))
            .build());
        this.sgMisc = this.settings.createGroup("Misc");
        //this.antiCollision = this.sgMisc.add(((BoolSetting.Builder) ((BoolSetting.Builder) (new BoolSetting.Builder())
        //    .name("AntiCollision"))
        //    .defaultValue(false))
        //    .build());
        this.debugMode = this.sgMisc.add(new BoolSetting.Builder()
            .name("Debug Mode")
            .description("Enable debug options for testing.")
            .defaultValue(false)
            .build());
        // 子调试开关（仅在 debugMode 开启时可见）
        this.debugOutput = this.sgMisc.add(new BoolSetting.Builder()
            .name("DebugOutput")
            .description("Output debug information to the console.")
            .defaultValue(false)
            .visible(this.debugMode::get)
            .build());
        this.debugNeverModifyGravity = this.sgMisc.add(new BoolSetting.Builder()
            .name("Debug: Never Modify Gravity")
            .description("Prevent any modifications to the player's gravity attribute.")
            .defaultValue(false)
            .visible(this.debugMode::get)
            .build());
        this.debugDisableGravityLock = this.sgMisc.add(new BoolSetting.Builder()
            .name("Debug: Disable Gravity Lock")
            .description("Force gravity to remain at 0.08 (vanilla) regardless of flight state.")
            .defaultValue(false)
            .visible(this.debugMode::get)
            .build());
        this.debugCompensationStrength = this.sgMisc.add(new DoubleSetting.Builder()
            .name("Debug: DoFly() Compensation Strength")
            .description("Upward compensation strength for level flight when looking up.")
            .defaultValue(0.005)
            .range(0.0, 0.1)
            .sliderRange(0.0, 0.05)
            .visible(this.debugMode::get)
            .build());
        this.debugCompensationDamping = this.sgMisc.add(new DoubleSetting.Builder()
            .name("Debug: DoFly() Compensation Damping")
            .description("Horizontal damping applied during compensation (1.0 = no damping).")
            .defaultValue(0.999)
            .range(0.9, 1.0)
            .sliderRange(0.98, 1.0)
            .visible(this.debugMode::get)
            .build());
        this.debugEnableCompensation = this.sgMisc.add(new BoolSetting.Builder()
            .name("Debug: Enable Compensation")
            .description("Enable upward compensation for level flight when looking up.")
            .defaultValue(false)
            .visible(this.debugMode::get)
            .build());
        this.debugNormalizeHorizontalLimitFactor = this.sgMisc.add(new DoubleSetting.Builder()
            .name("Debug: Normalize Limit Factor")
            .description("Multiplier for the horizontal speed limit during normalization.")
            .defaultValue(1.0)
            .range(0.5, 2.0)
            .sliderRange(0.5, 2.0)
            .visible(this.debugMode::get)
            .build()
        );

        this.debugSmoothFactorOverride = this.sgMisc.add(new DoubleSetting.Builder()
            .name("Debug: Smooth Factor Override")
            .description("Override smooth factor when Smooth Velocity is enabled (0.0 = no override).")
            .defaultValue(0.0)
            .range(0.0, 1.0)
            .sliderRange(0.0, 1.0)
            .visible(this.debugMode::get)
            .build()
        );

        this.debugDoNormalFlyDragX = this.sgMisc.add(new DoubleSetting.Builder()
            .name("Debug: doNormalFly Drag X")
            .description("Horizontal drag multiplier in doNormalFly (default ~0.99).")
            .defaultValue(0.9900000095367432)
            .range(0.9, 1.0)
            .sliderRange(0.95, 1.0)
            .visible(this.debugMode::get)
            .build()
        );

        this.debugDoNormalFlyDragY = this.sgMisc.add(new DoubleSetting.Builder()
            .name("Debug: doNormalFly Drag Y")
            .description("Vertical drag multiplier in doNormalFly (default ~0.98).")
            .defaultValue(0.9800000190734863)
            .range(0.9, 1.0)
            .sliderRange(0.95, 1.0)
            .visible(this.debugMode::get)
            .build()
        );

        this.debugDoNormalFlyDragZ = this.sgMisc.add(new DoubleSetting.Builder()
            .name("Debug: doNormalFly Drag Z")
            .description("Horizontal drag multiplier in doNormalFly (default ~0.99).")
            .defaultValue(0.9900000095367432)
            .range(0.9, 1.0)
            .sliderRange(0.95, 1.0)
            .visible(this.debugMode::get)
            .build()
        );

        this.debugAlienV4RawUpMultiplier = this.sgMisc.add(new DoubleSetting.Builder()
            .name("Debug: AlienV4 RawUp Multiplier")
            .description("Multiplier for rawUp calculation (hSpeed * this).")
            .defaultValue(0.01325)
            .range(0.005, 0.05)
            .sliderRange(0.01, 0.03)
            .visible(this.debugMode::get)
            .build()
        );

        this.debugAlienV4UpSpeedMultiplier = this.sgMisc.add(new DoubleSetting.Builder()
            .name("Debug: AlienV4 UpSpeed Multiplier")
            .description("Multiplier for final upSpeed (rawUp * this).")
            .defaultValue(3.2)
            .range(1.0, 6.0)
            .sliderRange(2.0, 4.0)
            .visible(this.debugMode::get)
            .build()
        );

        this.startTimer = new Timer();
        this.playerLastY = 114514.0D;
        //this.NO_OPERATION = -999.0F;
        this.pos = 0.0F;
        this.fangx = 1;
        this.fakeYaw = 0.0F;
        this.offsetYaw = 0.0F;
        this.vec = Vec3.ZERO;
        this.oVec = Vec3.ZERO;
        this.useTimer = new Timer();
        //this.gravityRestored = false;
        //this.shouldHover = false;//悬停功能修复
    }

    @EventHandler
    public void onTravel(TravelEvent event) {
        /*this.oVec = new Vec3(this.vec.x, this.vec.y, this.vec.z);
        this.vec = new Vec3(this.mc.player.getX(), this.mc.player.getY(), this.mc.player.getZ());
        if (isFlying()) {
            doFly(event);
            return;
        }
        takeoff(event);*/
        if (mc.player == null || !mc.player.isLocalPlayer()) return;
        this.oVec = new Vec3(this.vec.x, this.vec.y, this.vec.z);
        this.vec = new Vec3(this.mc.player.getX(), this.mc.player.getY(), this.mc.player.getZ());

        // ✅ 空中自动进入滑翔状态（只要穿着鞘翅、在空中且下落，autoStart开启则触发）
        /*if (!isFlying() && isElytraOn() && !mc.player.onGround() && this.autoStart.get()) {
            if (this.mc.player.getDeltaMovement().y < 0) { // 确保下落
                if (!this.packetStart.get()) {
                    this.mc.player.startFallFlying();
                } else {
                    this.mc.player.connection.send(new ServerboundMovePlayerPacket.PosRot(
                        this.mc.player.getX(), this.mc.player.getY(), this.mc.player.getZ(),
                        this.mc.player.getYRot(), this.mc.player.getXRot(),
                        this.mc.player.onGround(), false
                    ));
                }
            }
        }*/

        if (isFlying()) {
            doFly(event);
            return;
        }
        // 如果未飞行，不再需要 takeoff 中的复杂判断，可以直接返回或保留原有 takeoff 作为备用（但建议删除）
    }

    // 修改 onPacketSend 中的字段缓存部分
    /*@EventHandler
    public void onPacketSend(PacketEvent.Send event) {
        if (mc.player == null || !isFlying()) return;
        if (!(isMoveBindPress() || mc.options.keyJump.isDown() || mc.options.keyShift.isDown())) return;
        if (!(event.packet instanceof ServerboundMovePlayerPacket)) return;
        
        boolean inFluid = mc.player.isInWater() || mc.player.isInLava();
        if (inFluid && !this.pitchSpoofInWater.get()) return;

        if (!this.pitchSpoof.get() || isBoost()) return;
        // 字段缓存（仅一次）
        if (pitchField == null) {
            try {
                Class<?> packetClass = Class.forName("net.minecraft.class_2828");
                pitchField = packetClass.getDeclaredField("field_12885");
                pitchField.setAccessible(true);
                DebugOutput("[SeijaElytra] Pitch field ready (field_12885)", ChatFormatting.GREEN);
            } catch (Exception e) {
                DebugOutput("[SeijaElytra] Failed to access pitch field", ChatFormatting.RED);
                return;
            }
        }

        try {
            // 保存原始值
            float original = pitchField.getFloat(event.packet);
            float target = this.fakePitch.get().floatValue();

            // 修改
            pitchField.setFloat(event.packet, target);

            // 在下一 tick 恢复（避免影响渲染）
            mc.execute(() -> {
                try {
                    pitchField.setFloat(event.packet, original);
                } catch (IllegalAccessException ignored) {}
            });

            if (mc.player.tickCount % 20 == 0) {
                DebugOutput("[SeijaElytra] Pitch spoofed: " + original + " -> " + target, ChatFormatting.GRAY);
            }
        } catch (IllegalAccessException ignored) {}
    }*/

    public void doFly(TravelEvent event) {
        event.isCancel = true; 
        boolean isOnGround = this.mc.player.onGround();

        if (this.debugMode.get() && this.debugDisableGravityLock.get()) {
            Objects.requireNonNull(this.mc.player.getAttribute(Attributes.GRAVITY)).setBaseValue(0.08D);
        } else {
            if (this.mc.options.keyShift.isDown() || isOnGround) {
                Objects.requireNonNull(this.mc.player.getAttribute(Attributes.GRAVITY)).setBaseValue(0.08D);
            } else {
                Objects.requireNonNull(this.mc.player.getAttribute(Attributes.GRAVITY)).setBaseValue((this.debugMode.get() && this.debugNeverModifyGravity.get()) ? 0.08D : 0.0D);
            }
        }

        float yaw = MathUtil.getYaw();
        float autoMoveYaw = calcAutoMoveYaw();
        boolean automove = false;
        Vec3 motion = new Vec3(0.0D, 0.0D, 0.0D);
        if (autoMoveYaw != -999.0F) {
            automove = true;
            yaw = autoMoveYaw;
        }
        this.fakeYaw = yaw = (this.mc.player.isInWater() || this.mc.player.isInLava()) ? (yaw + updateWaterYawOff(this.waterYawSpeed.get().floatValue())) : yaw;
        float pitch = (float)((this.mc.player.isInWater() || this.mc.player.isInLava()) ? -this.waterPitch.get().doubleValue() : -this.upPitch.get().doubleValue());
        double accSpeed = ((this.mc.player.isInWater() || this.mc.player.isInLava()) ? this.waterAccelerateSpeed.get() : this.accelerateSpeed.get()).doubleValue();
        
        if (this.mc.options.keyJump.isDown()) {
            if (isMoveBindPress()) {
                motion = motion.add(riseHeight(event, yaw, pitch, accSpeed));
            } else {
                motion = motion.add(riseHeight(event, this.offsetYaw, pitch, accSpeed));
            }
        } else {
            Vec3 move = move(yaw, event, automove);
            motion = motion.add(move);
            motion = motion.add(downMove(event));
        }

        // 抬头平飞补偿（仅在 Debug 模式且启用时生效）
        if (this.debugMode.get() && this.debugEnableCompensation.get()) {
            if (!isBoost() && !mc.player.onGround() && isMoveBindPress()) {
                boolean hasVerticalInput = mc.options.keyJump.isDown() || mc.options.keyShift.isDown();
                boolean lookingUp = mc.player.getXRot() < 0;
                boolean inFluid = mc.player.isInWater() || mc.player.isInLava();
                if (lookingUp && !hasVerticalInput && !inFluid) {
                    float currentPitch = mc.player.getXRot();
                    double compensation = this.debugCompensationStrength.get() * (-currentPitch / 45.0);
                    double damping = this.debugCompensationDamping.get();
                    compensation = Mth.clamp(compensation, 0.0, 0.1);
                    motion = motion.add(0, compensation, 0);
                    motion = motion.multiply(damping, 1.0, damping);
                }
            }
        }
        setPos(event, motion);
    }

    public Vec3 downMove(TravelEvent event) {
        double motionY = -(this.GlideSpeed.get().doubleValue() / 10000.0D);
        if (this.mc.options.keyShift.isDown()){
            motionY = -this.DownSpeed.get().doubleValue();
        }
        return new Vec3(0.0D, motionY, 0.0D);
    }

    public Vec3 riseHeight(TravelEvent event, float yaw, float pitch, double accSpeed) {
        if (this.verticalTakeoff.get().booleanValue()) {
            if (isBoost())
                return upMove().add(move(yaw, event, false));
            if (this.autoUse.get().booleanValue())
                if (this.fireWorkDelay.get().doubleValue() <= 20.0D) {
                    useFirework();
                } else {
                    autoUse();
                }
        }
        // 如果启用了 AlienV4RiseMethod，采用新的动态升力算法
        if (this.AlienV4RiseMethod.get()) {
            Vec3 horizontal;
            if (isMoveBindPress()) {
                horizontal = move(yaw, event, false);
            } else {
                horizontal = move(this.offsetYaw, event, true);
            }

            double hSpeed = Math.sqrt(horizontal.x * horizontal.x + horizontal.z * horizontal.z);
            double upSpeed;
            if (hSpeed > 0.5) {
                Vec3 lookVec = getRotationVector(pitch, yaw);
                double lookDist = Math.sqrt(lookVec.x * lookVec.x + lookVec.z * lookVec.z);
                if (lookDist > 0.0) {
                    double rawUp = hSpeed * (this.debugMode.get() ? this.debugAlienV4RawUpMultiplier.get() : 0.01325);
                    upSpeed = rawUp * (this.debugMode.get() ? this.debugAlienV4UpSpeedMultiplier.get() : 3.2);
                    horizontal = horizontal.subtract(lookVec.x * rawUp / lookDist, 0, lookVec.z * rawUp / lookDist);
                } else {
                    upSpeed = this.accelerateSpeed.get() * 0.5;
                }
            } else {
                upSpeed = this.accelerateSpeed.get() * 0.5;
            }
            return horizontal.add(0, upSpeed, 0);
        }
        Vec3 sp = getSpeed();
        double l_MotionSq = Math.sqrt(sp.x * sp.x + sp.z * sp.z);
        if (l_MotionSq > accSpeed)
            return doNormalFly(pitch, yaw);
        changeOffsetYaw();
        return move(yaw, event, !isMoveBindPress());
    }

    public void DebugOutput(String message) {
        DebugOutput(message, ChatFormatting.WHITE);
    }
    public void DebugOutput(String message,ChatFormatting color) {
        if (this.debugOutput.get() && this.debugMode.get()) {
            ChatUtils.sendMsg(Component.literal("DonkeySpawnerElytraFlyDebug]" + message).withStyle(color));
        }
    }

    public Vec3 doNormalFly(float pitch, float yaw) {
        //this.mc.player.setSharedFlag(7, true); // Elytra flag (was method_45318)
        this.mc.player.startFallFlying();
        Vec3 vec3d4 = this.mc.player.getDeltaMovement();
        Vec3 vec3d5 = getRotationVector(pitch, yaw);
        float f = pitch * 0.017453292F;
        double i = Math.sqrt(vec3d5.x * vec3d5.x + vec3d5.z * vec3d5.z);
        double j = vec3d4.horizontalDistance();
        double k = vec3d5.length();
        double l = Math.cos(f);
        l = l * l * Math.min(1.0D, k / 0.4D);
        vec3d4 = this.mc.player.getDeltaMovement().add(0.0D, 0.08D * (-1.0D + l * 0.75D), 0.0D);
        if (vec3d4.y < 0.0D && i > 0.0D) {
            double m = vec3d4.y * -0.1D * l;
            vec3d4 = vec3d4.add(vec3d5.x * m / i, m, vec3d5.z * m / i);
        }
        if (f < 0.0F && i > 0.0D) {
            double m = j * -Mth.sin(f) * 0.04D;
            vec3d4 = vec3d4.add(-vec3d5.x * m / i, m * 3.2D, -vec3d5.z * m / i);
        }
        if (i > 0.0D)
            vec3d4 = vec3d4.add((vec3d5.x / i * j - vec3d4.x) * 0.1D, 0.0D, (vec3d5.z / i * j - vec3d4.z) * 0.1D);
        double dragX = this.debugMode.get() ? this.debugDoNormalFlyDragX.get() : 0.99D;
        double dragY = this.debugMode.get() ? this.debugDoNormalFlyDragY.get() : 0.98D;
        double dragZ = this.debugMode.get() ? this.debugDoNormalFlyDragZ.get() : 0.99D;
        return vec3d4.multiply(dragX, dragY, dragZ);
    }

    public Vec3 move(float yaw, TravelEvent event, boolean autoMove) {
        if (!autoMove) {
            yaw = (float)(this.mc.player.yRotO + (this.mc.player.getYRot() - this.mc.player.yRotO) * 1.0f);
        }

        boolean inWater = this.mc.player.isInWater() || this.mc.player.isInLava();
        double currentSpeed;

        // 确定基础速度
        if (inWater) {
            currentSpeed = this.waterSpeed.get();
        } else {
            currentSpeed = this.speed.get();
        }

        // 详细速度控制
        if (isMoveBindPress()) {
            if (inWater && this.detailedHorizontalSpeedWater.get()) {
                currentSpeed = getDetailedSpeedWater();
            } else if (!inWater && this.detailedHorizontalSpeed.get()) {
                currentSpeed = getDetailedSpeed();
            }
        }

        // 计算水平方向向量（不包含垂直分量）
        double[] dir = MathUtil.transformStrafe(currentSpeed, autoMove, yaw);
        double motionX = dir[0];
        double motionZ = dir[1];

        // 垂直速度不再在此方法中计算，交由 downMove 和 riseHeight 处理
        // 归一化（仅对水平速度生效）
        if (this.normalizeSpeed.get()) {
            double limit = inWater ? this.waterSpeed.get() : this.speed.get();
            if (this.debugMode.get()) {
                limit *= this.debugNormalizeHorizontalLimitFactor.get();
            }
            double hSpeed = Math.sqrt(motionX * motionX + motionZ * motionZ);

            if (this.normalizeHorizontalOnly.get()) {
                if (hSpeed > limit) {
                    double scale = limit / hSpeed;
                    motionX *= scale;
                    motionZ *= scale;
                }
            } else {
                // 如果未来需要包含垂直速度的归一化，此处应传入 motionY，但当前 move 不产生垂直速度
                // 保留逻辑以备后用
            }
        }

        // 垂直速度置零，由外部叠加
        return new Vec3(motionX, 0.0, motionZ);
    }
    private double getDetailedSpeed() {
        boolean w = mc.options.keyUp.isDown();
        boolean s = mc.options.keyDown.isDown();
        boolean a = mc.options.keyLeft.isDown();
        boolean d = mc.options.keyRight.isDown();
        boolean shift = mc.options.keyShift.isDown();
        boolean jump = mc.options.keyJump.isDown();

        // 优先处理带垂直键的组合（Shift 优先于 Jump，可按需调整）
        if (shift) {
            if (w && !s && !a && !d) return speedForwardShift.get();
            if (s && !w && !a && !d) return speedBackShift.get();
            if (a && !w && !s && !d) return speedLeftShift.get();
            if (d && !w && !s && !a) return speedRightShift.get();
            // 如有需要可添加斜向+Shift，但为了简洁，可回退到纯方向或全局
        }
        if (jump) {
            if (w && !s && !a && !d) return speedForwardJump.get();
            if (s && !w && !a && !d) return speedBackJump.get();
            if (a && !w && !s && !d) return speedLeftJump.get();
            if (d && !w && !s && !a) return speedRightJump.get();
        }
        // 无垂直键时，处理纯水平组合（原有逻辑）
        if (w && a && !s && !d) return speedForwardLeft.get();
        if (w && d && !s && !a) return speedForwardRight.get();
        if (s && a && !w && !d) return speedBackLeft.get();
        if (s && d && !w && !a) return speedBackRight.get();

        if (w && !s && !a && !d) return speedForward.get();
        if (s && !w && !a && !d) return speedBack.get();
        if (a && !w && !s && !d) return speedLeft.get();
        if (d && !w && !s && !a) return speedRight.get();

        return this.speed.get();
    }

    private double getDetailedSpeedWater() {
        boolean w = mc.options.keyUp.isDown();
        boolean s = mc.options.keyDown.isDown();
        boolean a = mc.options.keyLeft.isDown();
        boolean d = mc.options.keyRight.isDown();
        boolean shift = mc.options.keyShift.isDown();
        boolean jump = mc.options.keyJump.isDown();

        // 优先处理带垂直键的组合
        if (shift) {
            if (w && !s && !a && !d) return speedForwardShiftWater.get();
            if (s && !w && !a && !d) return speedBackShiftWater.get();
            if (a && !w && !s && !d) return speedLeftShiftWater.get();
            if (d && !w && !s && !a) return speedRightShiftWater.get();
        }
        if (jump) {
            if (w && !s && !a && !d) return speedForwardJumpWater.get();
            if (s && !w && !a && !d) return speedBackJumpWater.get();
            if (a && !w && !s && !d) return speedLeftJumpWater.get();
            if (d && !w && !s && !a) return speedRightJumpWater.get();
        }

        // 无垂直键时，处理纯水平组合
        if (w && a && !s && !d) return speedForwardLeftWater.get();
        if (w && d && !s && !a) return speedForwardRightWater.get();
        if (s && a && !w && !d) return speedBackLeftWater.get();
        if (s && d && !w && !a) return speedBackRightWater.get();

        if (w && !s && !a && !d) return speedForwardWater.get();
        if (s && !w && !a && !d) return speedBackWater.get();
        if (a && !w && !s && !d) return speedLeftWater.get();
        if (d && !w && !s && !a) return speedRightWater.get();

        return this.waterSpeed.get();
    }

    /*private void setPos(TravelEvent e, Vec3 motion) {
        if (this.smoothVelocity.get()) {
            Vec3 currentMotion = this.mc.player.getDeltaMovement();
            double factor = this.smoothFactor.get();
            if (this.debugMode.get() && this.debugSmoothFactorOverride.get() > 0.0) {
                factor = this.debugSmoothFactorOverride.get();
            }
            // 对水平速度和垂直速度分别插值（垂直可独立控制，此处统一）
            double newX = currentMotion.x + (motion.x - currentMotion.x) * factor;
            double newY = currentMotion.y + (motion.y - currentMotion.y) * factor;
            double newZ = currentMotion.z + (motion.z - currentMotion.z) * factor;

            this.mc.player.setDeltaMovement(newX, newY, newZ);
        } else {
            this.mc.player.setDeltaMovement(motion);
        }
        this.mc.player.hurtMarked = true;
    }*/
    private void setPos(TravelEvent e,Vec3 motion) {
        // 平滑处理（如果启用）
        if (this.smoothVelocity.get()) {
            Vec3 current = mc.player.getDeltaMovement();
            double factor = this.smoothFactor.get();
            if (this.debugMode.get() && this.debugSmoothFactorOverride.get() > 0.0) {
                factor = this.debugSmoothFactorOverride.get();
            }
            motion = new Vec3(
                current.x + (motion.x - current.x) * factor,
                current.y + (motion.y - current.y) * factor,
                current.z + (motion.z - current.z) * factor
            );
        }

        // 使用 move 应用整个运动向量，这是唯一稳定生效的方式
        mc.player.move(MoverType.SELF, motion);

        // 同步速度变量，用于其他计算
        mc.player.setDeltaMovement(motion);
        mc.player.hurtMarked = true;
    }


    /*private void takeoff(PlayerTickMovementEvent event) {
        boolean horizontalCollision=false;
        if (this.mc.player.onGround())
            this.playerLastY = this.mc.player.getY();
        if (isFlying())
            this.playerLastY = -999.0D;
        if (!isFlying() && isElytraOn() && !this.mc.player.onGround() && this.autoStart.get() && !(this.mc.player.isInWater()||this.mc.player.isInLava()) && this.mc.player.getY() - this.playerLastY > this.startY.get().doubleValue() && this.startTimer.passedDms(this.tryTakeoffDelay.get().doubleValue())) {
            this.startTimer.reset();
            if (!this.packetStart.get().booleanValue()) {
                this.mc.player.startFallFlying();
            } else {
                this.mc.player.connection.send(new ServerboundMovePlayerPacket.PosRot(
                    this.mc.player.getX(),
                    this.mc.player.getY(),
                    this.mc.player.getZ(),
                    this.mc.player.getYRot(),
                    this.mc.player.getXRot(),
                    this.mc.player.onGround(),
                    horizontalCollision
                ));
            }
            return;
        }
    }*/

    public void onActivate() {
        //if (this.autoStart.get() && isElytraOn() && !isFlying() && !this.mc.player.onGround() && this.mc.player != null)
        //    this.mc.player.startFallFlying();
    }

    public void onDeactivate() {
        if (this.mc.player != null) {
            // 恢复重力为默认值
            Objects.requireNonNull(this.mc.player.getAttribute(Attributes.GRAVITY)).setBaseValue(0.08D);
            //gravityRestored = false;
            // 结束鞘翅飞行状态
            //this.mc.player.stopFallFlying();
            // 清零速度，防止落地滑行
            //this.mc.player.setDeltaMovement(Vec3.ZERO);
            // 重置 onGround 标志（可选）
            //this.mc.player.setOnGround(false);
        }
    }

    public boolean isElytraOn() {
        ItemStack chest = this.mc.player.getItemBySlot(EquipmentSlot.CHEST);
        //DebugOutput("[DEBUG] isElytraOn() = " + chest.has(DataComponents.GLIDER) + ", chest item: " + chest);
        // 1.21.11 数据驱动判定：检查鞘翅飞行能力
        return /*chest.getItem() instanceof ElytraItem || */chest.has(DataComponents.GLIDER);
    }

    private boolean isFlying() {
        return this.mc.player.isFallFlying();
    }

    private boolean isMoveBindPress() {
        return (this.mc.options.keyUp.isDown() || this.mc.options.keyDown.isDown() || this.mc.options.keyLeft.isDown() || this.mc.options.keyRight.isDown());
    }

    public Vec3 getSpeed() {
        return new Vec3(this.vec.x - this.oVec.x, this.vec.y - this.oVec.y, this.vec.z - this.oVec.z);
    }

    protected final Vec3 getRotationVector(float pitch, float yaw) {
        float f = pitch * 0.017453292F;
        float g = -yaw * 0.017453292F;
        float h = Mth.cos(g);
        float i = Mth.sin(g);
        float j = Mth.cos(f);
        float k = Mth.sin(f);
        return new Vec3((i * j), -k, (h * j));
    }

    public static final Vec3 getVectorForRotation(float pitch, float yaw) {
        float f = Mth.cos(-yaw * 0.017453292F - 3.1415927F);
        float f1 = Mth.sin(-yaw * 0.017453292F - 3.1415927F);
        float f2 = -Mth.cos(-pitch * 0.017453292F);
        float f3 = Mth.sin(-pitch * 0.017453292F);
        return new Vec3((f1 * f2), f3, (f * f2));
    }

    private float calcAutoMoveYaw() {
        float yaw = -999.0F;
        if (this.autoPlane.get() && this.mc.player.getY() > this.autoPlaneY.get() && !isMoveBindPress() && !this.mc.options.keyJump.isDown()) {
            Double x = null;
            Double z = null;
            try {
                x = Double.valueOf(Double.parseDouble(this.destinationX.get()));
                z = Double.valueOf(Double.parseDouble(this.destinationZ.get()));
            } catch (NumberFormatException numberFormatException) {}
            x = Double.valueOf((x == null) ? this.mc.player.getX() : x.doubleValue());
            z = Double.valueOf((z == null) ? this.mc.player.getZ() : z.doubleValue());
            Vec3 destination = new Vec3(x.doubleValue(), this.mc.player.getY(), z.doubleValue());
            if (Math.sqrt(this.mc.player.distanceToSqr(destination.x, destination.y, destination.z)) > 40.0D) {
                yaw = getLegitRotations(destination)[0];
            } else if (this.toggleAutoPlane.get()) {
                this.autoPlane.set(false);
            }
        }
        if (this.playerDodge.get() && !isMoveBindPress() && !this.mc.options.keyJump.isDown()) {
            List<AbstractClientPlayer> l = this.mc.level.players().stream().filter(player -> (this.mc.player.distanceTo(player) <= 16.0F && !this.mc.player.equals(player) && !Friends.get().shouldAttack(player))).collect(Collectors.toList());
            l.sort((p, p1) -> (int)(this.mc.player.distanceTo(p) - this.mc.player.distanceTo(p1)));
            if (l.size() != 0)
                yaw = getLegitRotations(l.get(0).position())[0] + 180.0F;
        }
        return yaw;
    }

    
/*
    public float updateWaterYawOff(float pow) {
        float i = this.pos + pow * this.fangx;
        if (i > 20.0F) {
            this.fangx *= -1;
            float pow2 = pow - 20.0F - this.pos;
            this.pos = 20.0F;
            return updateWaterYawOff(pow2);
        }
        if (i < -20.0F) {
            this.fangx *= -1;
            float pow2 = pow - this.pos + 20.0F;
            this.pos = -20.0F;
            return updateWaterYawOff(pow2);
        }
        this.pos = i;
        return i;
    }
*/

    public float updateWaterYawOff(float pow) {//修复递归导致的栈溢出
        float i = this.pos + pow * this.fangx;
        while (i > 20.0F || i < -20.0F) {
            if (i > 20.0F) {
                this.fangx *= -1;
                float over = i - 20.0F;
                this.pos = 20.0F;
                i = this.pos + over * this.fangx;
            } else if (i < -20.0F) {
                this.fangx *= -1;
                float over = i + 20.0F;
                this.pos = -20.0F;
                i = this.pos + over * this.fangx;
            }
        }
        this.pos = i;
        return i;
    }

    public void changeOffsetYaw() {
        this.offsetYaw = (float)(this.offsetYaw + this.yawSpeed.get().doubleValue());
        if (this.offsetYaw >= 360.0F)
            this.offsetYaw = 0.0F;
    }

    public float[] getLegitRotations(Vec3 vec) {
        Vec3 eyesPos = this.mc.player.getEyePosition();
        double diffX = vec.x - eyesPos.x;
        double diffY = vec.y - eyesPos.y;
        double diffZ = vec.z - eyesPos.z;
        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);
        float yaw = (float)Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
        float pitch = (float)-Math.toDegrees(Math.atan2(diffY, diffXZ));
        return new float[] { this.mc.player.getYHeadRot() + Mth.wrapDegrees(yaw - this.mc.player.getYHeadRot()), this.mc.player.getXRot() + Mth.wrapDegrees(pitch - this.mc.player.getXRot()) };
    }

    /* 
    @EventHandler
    public void onPacketSend(PacketEvent.Send event) {
        if (pitchField == null) {
            //ChatUtils.sendMsg(Component.literal("[SeijaElytra] Warning: Packet pitch spoofing is unavailable due to missing field.").withStyle(ChatFormatting.YELLOW));
            return;
        }               // 字段不存在，跳过
        if (mc.player == null || !isFlying()) {
            //ChatUtils.sendMsg(Component.literal("[SeijaElytra] Warning: Player not flying, skipping pitch spoofing.").withStyle(ChatFormatting.YELLOW));
            return;
        }
        // 如果在流体中且未开启水中欺骗，则跳过
        boolean inFluid = mc.player.isInWater() || mc.player.isInLava();
        if (inFluid && !this.pitchSpoofInWater.get()) {
            //ChatUtils.sendMsg(Component.literal("[SeijaElytra] Warning: Player in fluid and water pitch spoofing disabled, skipping.").withStyle(ChatFormatting.YELLOW));
            return;
        }
        if (!this.pitchSpoof.get() || isBoost()) {
            //ChatUtils.sendMsg(Component.literal("[SeijaElytra] Pitch spoofing disabled or boost active, skipping.").withStyle(ChatFormatting.YELLOW));
            return;
        }
        if (!(isMoveBindPress() || mc.options.keyJump.isDown() || mc.options.keyShift.isDown())) {
            //ChatUtils.sendMsg(Component.literal("[SeijaElytra] No movement input detected, skipping pitch spoofing.").withStyle(ChatFormatting.YELLOW));
            return;
        }
        if (!(event.packet instanceof ServerboundMovePlayerPacket)) {
            //ChatUtils.sendMsg(Component.literal("[SeijaElytra] Non-movement packet detected, skipping pitch spoofing.").withStyle(ChatFormatting.YELLOW));
            return;
        }

        try {
            //float originalPitch = pitchField.getFloat(event.packet);
            //float targetPitch = this.fakePitch.get().floatValue();

            // 仅当需要修改时才输出，避免刷屏（每20 tick输出一次）
            //if (mc.player.tickCount % 20 == 0) {
                //ChatUtils.sendMsg(Component.literal("[SeijaElytra] Original Pitch: " + originalPitch + " -> Target: " + targetPitch).withStyle(ChatFormatting.GREEN));
            //}
            pitchField.setFloat(event.packet, this.fakePitch.get().floatValue());
        } catch (IllegalAccessException e) {
            // setAccessible(true) 已调用，此异常永远不会发生
        }
    }*/
    
    @EventHandler
    public void onTick(TickEvent.Pre event) {
        if (this.mc.player == null)
            return;
        // === 自动进入平飞检测（持续尝试）===
        if (this.autoStart.get() && !isFlying() && isElytraOn() && !mc.player.onGround()&& (this.startY.get()==0.0d || (mc.player.getDeltaMovement().y <= -this.startY.get()))) {
            //DebugOutput("mc.player.onGround():" + (mc.player.onGround()));
            long now = System.currentTimeMillis();
            if (now - lastAutoStartAttempt > AUTO_START_COOLDOWN_MS) {
                lastAutoStartAttempt = now;
                // 模拟真实跳跃触发
                simulateJumpAndStartFlying();
            }
        }
        // 防御性重力锁定：飞行中且无垂直输入、不在地面时，强制保持重力为0
        if (isFlying() && !mc.player.onGround()) {
            boolean hasVerticalInput = mc.options.keyJump.isDown() || mc.options.keyShift.isDown();
            if (!hasVerticalInput) {
                Objects.requireNonNull(mc.player.getAttribute(Attributes.GRAVITY)).setBaseValue((this.debugMode.get() && this.debugNeverModifyGravity.get()) ? 0.08D : 0.0D);
            }
        }

        // 检测同步按钮是否被按下
        if (this.syncDetailedSpeeds.get()) {
            double global = this.speed.get();
            // 原有八个方向
            
            this.speedForward.set(global);
            this.speedBack.set(global);
            this.speedLeft.set(global);
            this.speedRight.set(global);
            this.speedForwardLeft.set(global);
            this.speedForwardRight.set(global);
            this.speedBackLeft.set(global);
            this.speedBackRight.set(global);
            // 新增带 Shift 的
            this.speedForwardShift.set(global);
            this.speedBackShift.set(global);
            this.speedLeftShift.set(global);
            this.speedRightShift.set(global);
            // 新增带 Jump 的
            this.speedForwardJump.set(global);
            this.speedBackJump.set(global);
            this.speedLeftJump.set(global);
            this.speedRightJump.set(global);
            
            this.syncDetailedSpeeds.set(false);
        }
        if (this.syncDetailedSpeedsWater.get()) {
            double globalWater = this.waterSpeed.get();
            this.speedForwardWater.set(globalWater);
            this.speedBackWater.set(globalWater);
            this.speedLeftWater.set(globalWater);
            this.speedRightWater.set(globalWater);
            this.speedForwardLeftWater.set(globalWater);
            this.speedForwardRightWater.set(globalWater);
            this.speedBackLeftWater.set(globalWater);
            this.speedBackRightWater.set(globalWater);
            // 带 Shift 的
            this.speedForwardShiftWater.set(globalWater);
            this.speedBackShiftWater.set(globalWater);
            this.speedLeftShiftWater.set(globalWater);
            this.speedRightShiftWater.set(globalWater);

            // 带 Shift 的
            this.speedForwardJumpWater.set(globalWater);
            this.speedBackJumpWater.set(globalWater);
            this.speedLeftJumpWater.set(globalWater);
            this.speedRightJumpWater.set(globalWater);

            this.syncDetailedSpeedsWater.set(false);
        }
    // === Pitch 欺骗（仅发包，不影响视角）===
        if (this.pitchSpoof.get() && this.mc.player.isFallFlying() && !isBoost()) {
            boolean inFluid = mc.player.isInWater() || mc.player.isInLava();
            if (inFluid && !this.pitchSpoofInWater.get()) {
                return;
            }

            boolean moving = isMoveBindPress() || mc.options.keyJump.isDown() || mc.options.keyShift.isDown();
            if (moving) {
                // 关键：Rotations.rotate 中正数为低头，负数为抬头
                // 我们要让服务器看到低头，所以传正数
                // 使用 Rotations.PRIORITY_HIGHEST + 1 确保覆盖其他模块
                Rotations.rotate(this.fakeYaw, this.fakePitch.get().doubleValue(),RotationPriority.get(),false,null);
            }
        }
    // === Pitch 低头修正（替代发包欺骗） ===
    /*     if (this.pitchSpoof.get() && this.mc.player.isFallFlying() && !isBoost()) {
            boolean inFluid = mc.player.isInWater() || mc.player.isInLava();
            if (inFluid && !this.pitchSpoofInWater.get()) {
                // 确保恢复原始视角（如果之前被修正过）
                if (pitchCorrected) {
                    mc.player.setXRot(originalPitch);
                    pitchCorrected = false;
                }
                return;
            }
            boolean hasVerticalInput = mc.options.keyJump.isDown() || mc.options.keyShift.isDown();
            boolean horizontalMoving = isMoveBindPress();
            float currentPitch = mc.player.getXRot();
            float maxSafePitch = this.fakePitch.get().floatValue(); // 注意：正数表示低头

            // 水平移动且抬头超过安全阈值（注意：负数表示抬头）
            if (horizontalMoving && !hasVerticalInput && currentPitch < -maxSafePitch) {
                if (!pitchCorrected) {
                    originalPitch = currentPitch;
                    pitchCorrected = true;
                }
                mc.player.setXRot(-maxSafePitch); // 强制低头到安全角度
            } else {
                // 非水平移动或已低头，恢复原始视角
                if (pitchCorrected) {
                    mc.player.setXRot(originalPitch);
                    pitchCorrected = false;
                }
            }
        } else {
            // 欺骗关闭或使用烟花时，确保恢复视角
            if (pitchCorrected) {
                mc.player.setXRot(originalPitch);
                pitchCorrected = false;
            }
        }
        /*if (this.pitchSpoof.get() && this.mc.player.isFallFlying() && !isBoost()) {
            boolean moving = isMoveBindPress() || mc.options.keyJump.isDown() || mc.options.keyShift.isDown();
            boolean lookingUp = mc.player.getXRot() < 0.0F;

            if (moving && lookingUp) {
                pitchSpoofTimer++;
                if (pitchSpoofTimer % 10 == 0) {
                    // 使用 fakePitch 作为低头目标角度
                    mc.player.setXRot(this.fakePitch.get().floatValue());
                    // 同时降低垂直速度
                    Vec3 vel = mc.player.getDeltaMovement();
                    mc.player.setDeltaMovement(vel.x, vel.y * 0.9, vel.z);
                }
            } else {
                pitchSpoofTimer = 0;
            }
        }*/
    }

    private void simulateJumpAndStartFlying() {
        // 获取当前客户端的按键状态
        ClientInput clientInput = mc.player.input;
        boolean forward = clientInput.keyPresses.forward();
        boolean backward = clientInput.keyPresses.backward();
        boolean left = clientInput.keyPresses.left();
        boolean right = clientInput.keyPresses.right();
        boolean shift = clientInput.keyPresses.shift();
        boolean sprint = clientInput.keyPresses.sprint();

        // 构造一个 Input 对象，跳跃键设为 true
        Input jumpInput = new Input(
            forward, backward, left, right,
            true,   // 模拟按下跳跃
            shift,
            sprint
        );

        // 发送按下跳跃的输入包
        mc.player.connection.send(new ServerboundPlayerInputPacket(jumpInput));

        // 发送开始滑翔命令
        mc.player.connection.send(new ServerboundPlayerCommandPacket(
            mc.player, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING
        ));

        // 在下一 tick 恢复原始输入（松开跳跃）
        mc.execute(() -> {
            Input originalInput = new Input(
                clientInput.keyPresses.forward(),
                clientInput.keyPresses.backward(),
                clientInput.keyPresses.left(),
                clientInput.keyPresses.right(),
                false,  // 松开跳跃
                clientInput.keyPresses.shift(),
                clientInput.keyPresses.sprint()
            );
            mc.player.connection.send(new ServerboundPlayerInputPacket(originalInput));
        });
    }

    public boolean isBoost() {
        if (this.mc.player == null || this.mc.level == null)
            return false;
        for (Entity e : this.mc.level.entitiesForRendering()) {
            if (!(e instanceof FireworkRocketEntity))
                continue;
            FireworkRocketEntity entity = (FireworkRocketEntity)e;
           // CompoundTag nbt = new CompoundTag();
            //entity.save(nbt);
            //if (nbt.getString("Owner").equals(this.mc.player.getStringUUID()))
            if (entity.getOwner() != null && entity.getOwner().is(this.mc.player))
                return true;
        }
        return false;
    }

    public Vec3 upMove() {
        Vec3 vec3d = getRotationVector(-90.0F, 0.0F);
        //double d = 1.5D;
        //double e = 0.1D;
        Vec3 vec3d2 = getSpeed().multiply(0.0D, 1.0D, 0.0D);
        Vec3 val = vec3d2.add(vec3d.x * 0.1D + (vec3d.x * 1.5D - vec3d2.x) * 0.5D, this.verticalMultiple.get().doubleValue() * (vec3d.y * 0.1D + (vec3d.y * 1.5D - vec3d2.y) * 0.5D), vec3d.z * 0.1D + (vec3d.z * 1.5D - vec3d2.z) * 0.5D);
        if (val.y > this.verticalMaxSpeed.get().doubleValue())
            val = val.multiply(1.0D, 0.0D, 1.0D).add(0.0D, this.verticalMaxSpeed.get().doubleValue(), 0.0D);
        return val;
    }

    public void useFirework() {
        if (!this.autoUse.get() && !this.useTimer.passed(this.fireWorkDelay.get().doubleValue()))
            return;
        this.useTimer.reset();
        if (this.mc.player.getMainHandItem().getItem() instanceof FireworkRocketItem) {
            this.mc.gameMode.useItem(this.mc.player, InteractionHand.MAIN_HAND);
        } else if (this.mc.player.getOffhandItem().getItem() instanceof FireworkRocketItem) {
            this.mc.gameMode.useItem(this.mc.player, InteractionHand.OFF_HAND);
        } else if (this.autoSwitch.get()) {
            //int selectedSlot = this.mc.player.getInventory().;
            int targetSlot = getStack(this.noSuicide.get());
            //System.out.println("DEBUG000000: Target firework slot " + targetSlot);
            if (targetSlot < 0||targetSlot > 35){
                //System.out.println("ERRRRRRRRRRRRRROR " + targetSlot);
                return;
            }else if(targetSlot<9){
                InvUtils.swap(targetSlot, this.silentSwitch.get());//有问题
                //System.out.println("DEBUG1111111: Switched to firework slot " + targetSlot);
                this.mc.gameMode.useItem(this.mc.player, InteractionHand.MAIN_HAND);
                this.mc.player.swing(InteractionHand.MAIN_HAND);
                if (this.silentSwitch.get()){
                    InvUtils.swapBack();
                }
            }else{
                // 找一个快捷栏空位，没有则使用当前选中槽位
                int hotbarSlot = -1;
                for (int i = 0; i < 9; i++) {
                    if (this.mc.player.getInventory().getItem(i).isEmpty()) {
                        hotbarSlot = i;
                        break;
                    }
                }
                if (hotbarSlot == -1) {
                    hotbarSlot = this.mc.player.getInventory().getSelectedSlot();
                }

                // 将背包槽位与目标快捷栏槽位交换
                InvUtils.move().from(targetSlot).to(hotbarSlot);

                // 切换到该快捷栏槽位并使用烟花
                InvUtils.swap(hotbarSlot, this.silentSwitch.get());
                this.mc.gameMode.useItem(this.mc.player, InteractionHand.MAIN_HAND);
                this.mc.player.swing(InteractionHand.MAIN_HAND);

                if (this.silentSwitch.get()) {
                    // 恢复手持槽位（回到使用前的选中槽位）
                    InvUtils.swapBack();
                    // 将交换的物品再交换回来，恢复原状
                    InvUtils.move().from(targetSlot).to(hotbarSlot);
                }
                // 若静默开关关闭，物品保持移动后的状态，不恢复
            }
        }
    }

    public int getStack(boolean noSuicide) {
        HashMap<Integer, Integer> map = new HashMap<>();
        for (int i = 0; i < (this.InventoryAutoSwitch.get()? 36 : 9); i++) {
            ItemStack stack = this.mc.player.getInventory().getItem(i);
            if (stack.getItem() instanceof FireworkRocketItem) {
                //int count = CrossBowTweaker.getStarCount(stack);
                //System.out.println("DEBUG000000: Found firework in slot " + i );
                Fireworks fireworks = stack.get(DataComponents.FIREWORKS);
                int flightDuration = (fireworks != null) ? fireworks.flightDuration() : 0;
                boolean hasExplosions = fireworks != null && fireworks.explosions().size() > 0;
                //System.out.println("DEBUG111111111111: Found firework in slot " + i + " with flight duration " + flightDuration);
                if (noSuicide){
                    if(!hasExplosions){
                        map.put(i, flightDuration);
                    }
                }else{
                    map.put(i, flightDuration);
                }
                //System.out.println("DEBUG: Found firework in slot " + i + " with flight duration " + flightDuration);                
            }
        }
        List<Map.Entry<Integer, Integer>> list = new ArrayList<>(map.entrySet().stream().toList());
        list.sort(Comparator.comparingInt(Map.Entry::getValue));
        if (list.size() == 0)
            return -1;
        return list.get(0).getKey();
    }

    public void autoUse() {
        if (this.useTimer.passed(this.fireWorkDelay.get().doubleValue()))
            useFirework();
    }
}
