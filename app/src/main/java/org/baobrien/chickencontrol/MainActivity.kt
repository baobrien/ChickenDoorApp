package org.baobrien.chickencontrol

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.content.Intent
import android.widget.*
import kotlinx.android.synthetic.main.activity_main.*
import android.util.Log;

import io.particle.android.sdk.cloud.*;
import io.particle.android.sdk.utils.*;
import java.util.*

class MainActivity : AppCompatActivity() {

    var particleDevice : ParticleDevice? = null;
    var oneUpdate : Int = 0;

    fun loginSetupDevice() {
        Log.wtf("tokenthing",ParticleCloudSDK.getCloud().getAccessToken())
        val distantFuture = Calendar.getInstance()
        distantFuture.add(Calendar.YEAR, 20);
        ParticleCloudSDK.getCloud().setAccessToken("<<>>",distantFuture.getTime())
        Log.wtf("tokenthing",ParticleCloudSDK.getCloud().getAccessToken())
        var devs = ParticleCloudSDK.getCloud().getDevices()
        if(devs.size < 1){
            throw Exception("Can't find any devices")
        }
        particleDevice = devs[0]
    }

    fun nullReadouts(){
        dsText.setText("Door status unknown")
    }

    fun couldNotConnectHandler(nextFun: (Exception?) -> Any = {}): (Exception?) -> Any {
        return {
            e: Exception? ->
            nullReadouts()
            Toast.makeText(getApplicationContext(), "Could not connect!", Toast.LENGTH_SHORT).show()
            Log.wtf("result", e.toString())
            nextFun(e)
        }
    }

    fun onLogin(doLogin: Boolean = true,  onSuccess: (Unit) -> Any = {}){
        if(doLogin) {
            KillerTask({ loginSetupDevice() }, onSuccess, couldNotConnectHandler()).go()
        } else {
            onSuccess(Unit)
        }
    }

    fun doDoorOpen(time: Int){
        onLogin {
            KillerTask({
                val argStr = if (time >= 0) time.toString() else "-1"
                val res = particleDevice?.callFunction("OpenTime", Py.list(argStr))
            }, {
                Toast.makeText(getApplicationContext(), "Door Opening", Toast.LENGTH_LONG).show()
                updateStatus(false)
            }, couldNotConnectHandler()).go()
        }
    }

    fun doDoorClose(time: Int){
        onLogin {
            KillerTask({
                val argStr = if (time >= 0) time.toString() else "-1"
                val res = particleDevice?.callFunction("CloseTime", Py.list(argStr))
            }, {
                Toast.makeText(getApplicationContext(), "Door Closing", Toast.LENGTH_LONG).show()
                updateStatus(false)
            }, couldNotConnectHandler()).go()
        }
    }

    fun doSunriseMode(){
        onLogin{
            KillerTask({ particleDevice?.callFunction("ResetDoor") }, {
                Toast.makeText(getApplicationContext(), "Door in sunrise/sunset mode", Toast.LENGTH_LONG).show()
                updateStatus(false)
            }, couldNotConnectHandler()).go()
        }
    }

    fun doSensorReading(doLogin: Boolean){
        onLogin{
            KillerTask<Double>({
                particleDevice?.getDoubleVariable("temperature")!!;
            },{
                temp ->
                temperatureText.setText("Temperature: %.2f °C".format(temp))
            },couldNotConnectHandler()).go();
        }
    }

    fun readDoorStatus(doLogin: Boolean, next: (Unit) -> Any = {}){
        onLogin(doLogin) {
            KillerTask<String>( { particleDevice?.getStringVariable("doorstatus")!!  },{
                doorStats ->
                Log.v("chickenDoorStatus", doorStats)

                val dsl = doorStats?.split(",")
                val doorCtl = dsl?.get(0)?.toInt()
                val doorAutoMode = dsl?.get(1)?.toInt()
                val doorOverrideCtl = dsl?.get(2)?.toInt()
                val doorOverrideTimeout = dsl?.get(3)?.toInt()
                val doorOverrideEnabled = dsl?.get(4)?.toInt()
                val lastSunState = dsl?.get(5)?.toInt()
                val deltat = dsl?.get(6)?.toInt()

                val statStr = when (doorCtl) {
                    0, 1 -> when (doorCtl) {
                        0 -> "Door Open "
                        else -> "Door Closed "
                    } + when (doorOverrideEnabled) {
                        1 -> "(remote control)"
                        else -> when (doorAutoMode) {
                            1 -> when (lastSunState) {
                                0 -> "(sun down)"
                                else -> "(sun up)"
                            }
                            else -> ""
                        }
                    }
                    else -> "Door reading sensor"
                }

                dsText.setText(statStr)

                next(Unit);
            }, couldNotConnectHandler(){next(Unit);}).go()
        }
    }



    fun updateStatus(doLogin: Boolean){
        if(oneUpdate == 0){
            oneUpdate++;
            RefreshStatusText.text = "Refreshing";
            readDoorStatus(doLogin){
                RefreshStatusText.text = ""
                Unit
            };
            doSensorReading(doLogin);
            oneUpdate = 0;
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus(true)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ParticleCloudSDK.init(this)
        nullReadouts()

        doButton.setOnClickListener{
            doDoorOpen(-1)
        }

        dotButton.setOnClickListener{
            doDoorOpen(15*60)
        }

        dcButton.setOnClickListener{
            doDoorClose(-1)
        }

        dctButton.setOnClickListener{
            doDoorClose(15*60)
        }

        dsButton.setOnClickListener{
            doSunriseMode()
        }

        rfButton.setOnClickListener{
            updateStatus(false)
        }
    }
}