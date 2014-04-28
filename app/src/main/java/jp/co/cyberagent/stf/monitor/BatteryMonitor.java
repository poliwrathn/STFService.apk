package jp.co.cyberagent.stf.monitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.Log;

import jp.co.cyberagent.stf.io.MessageWriter;
import jp.co.cyberagent.stf.proto.Wire;

public class BatteryMonitor extends AbstractMonitor {
    private static final String TAG  = "STFBatteryMonitor";

    private BatteryState state = null;

    public BatteryMonitor(Context context, MessageWriter.Pool writer) {
        super(context, writer);
    }

    @Override
    public void run() {
        Log.i(TAG, "Monitor starting");

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                state = new BatteryState(intent);
                report();
            }
        };

        context.registerReceiver(receiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        try {
            synchronized (this) {
                while (!isInterrupted()) {
                    wait();
                }
            }
        }
        catch (InterruptedException e) {
            // Okay
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            Log.i(TAG, "Monitor stopping");

            context.unregisterReceiver(receiver);
        }
    }

    @Override
    public void peek() {
        if (state != null) {
            report();
        }
    }

    private void report() {
        Log.i(TAG, String.format("Battery is %s (%s health); connected via %s; level at %d/%d; temp %.1fC@%.3fV",
                statusLabel(state.status),
                healthLabel(state.health),
                pluggedLabel(state.plugged),
                state.level,
                state.scale,
                state.temp / 10.0,
                state.voltage / 1000.0
        ));

        writer.write(Wire.Envelope.newBuilder()
                .setType(Wire.MessageType.EVENT_BATTERY)
                .setMessage(Wire.BatteryEvent.newBuilder()
                        .setStatus(statusLabel(state.status))
                        .setHealth(healthLabel(state.health))
                        .setPlugged(pluggedLabel(state.plugged))
                        .setLevel(state.level)
                        .setScale(state.scale)
                        .setTemp(state.temp / 10.0)
                        .setVoltage(state.voltage / 1000.0)
                        .build()
                        .toByteString())
                .build());
    }

    private String healthLabel(int health) {
        switch (health) {
            case BatteryManager.BATTERY_HEALTH_COLD:
                return "cold";
            case BatteryManager.BATTERY_HEALTH_GOOD:
                return "good";
            case BatteryManager.BATTERY_HEALTH_DEAD:
                return "dead";
            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
                return "over_voltage";
            case BatteryManager.BATTERY_HEALTH_OVERHEAT:
                return "overheat";
            case BatteryManager.BATTERY_HEALTH_UNKNOWN:
                return "unknown";
            case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE:
                return "unspecified_failure";
            default:
                return "unknown_" + health;
        }
    }

    private String pluggedLabel(int plugged) {
        switch (plugged) {
            case BatteryManager.BATTERY_PLUGGED_AC:
                return "ac";
            case BatteryManager.BATTERY_PLUGGED_USB:
                return "usb";
            case BatteryManager.BATTERY_PLUGGED_WIRELESS:
                return "wireless";
            default:
                return "unknown_" + plugged;
        }
    }

    private String statusLabel(int status) {
        switch (status) {
            case BatteryManager.BATTERY_STATUS_CHARGING:
                return "charging";
            case BatteryManager.BATTERY_STATUS_DISCHARGING:
                return "discharging";
            case BatteryManager.BATTERY_STATUS_FULL:
                return "full";
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                return "not_charging";
            case BatteryManager.BATTERY_STATUS_UNKNOWN:
                return "unknown";
            default:
                return "unknown_" + status;
        }
    }

    private static class BatteryState {
        private int health;
        private int level;
        private int plugged;
        private int scale;
        private int status;
        private String tech;
        private int temp;
        private int voltage;

        public BatteryState(Intent intent) {
            health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN);
            level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
            scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 0);
            status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN);
            tech = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY);
            temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
            voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
        }
    }
}