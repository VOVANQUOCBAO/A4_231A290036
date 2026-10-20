package vn.edu.vhu.ltdd.a4events;

import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.ActivityInfo;
import android.graphics.*;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.io.*;
import java.util.*;

/** Runs the submitted scenarios on the device; no test hooks in the release app. */
public class LabRunner extends Instrumentation {
    private volatile Activity active;
    private final ArrayList<String> results=new ArrayList<>();
    private String mode="full";
    @Override public void onCreate(Bundle arguments) {
        super.onCreate(arguments);
        if(arguments!=null) mode=arguments.getString("mode","full");
        start();
    }
    @Override public void onStart() {
        try {
            ((Application)getTargetContext().getApplicationContext()).registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
                public void onActivityResumed(Activity a){active=a;}
                public void onActivityCreated(Activity a,Bundle b){active=a;}
                public void onActivityStarted(Activity a){}
                public void onActivityPaused(Activity a){}
                public void onActivityStopped(Activity a){}
                public void onActivitySaveInstanceState(Activity a,Bundle b){}
                public void onActivityDestroyed(Activity a){}
            });
            Intent intent=new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK);
            active=startActivitySync(intent); settle();
            execute();
            save("results.txt",android.text.TextUtils.join("\n",results)+"\nALL PASSED\n");
            Bundle done=new Bundle();done.putString("stream","\n"+android.text.TextUtils.join("\n",results)+"\nALL PASSED\n");finish(Activity.RESULT_OK,done);
        } catch(Throwable e) {
            StringWriter sw=new StringWriter();e.printStackTrace(new PrintWriter(sw));
            try { save("results.txt",android.text.TextUtils.join("\n",results)+"\nFAILED\n"+sw); shot("FAILED"); } catch(Throwable ignored){}
            Bundle b=new Bundle();b.putString("stream","FAILED: "+sw);finish(Activity.RESULT_CANCELED,b);
        }
    }
    private void ui(Runnable action) { runOnMainSync(action); waitForIdleSync(); }
    private void pause(long ms) { SystemClock.sleep(ms); }
    private void settle(){waitForIdleSync();pause(900);}
    private View view(int id){return active.findViewById(id);}
    private void hideKeyboard(){ui(()->((InputMethodManager)active.getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(active.getWindow().getDecorView().getWindowToken(),0));}
    private void show(int id) {
        hideKeyboard(); ui(()->{View v=view(id);v.requestRectangleOnScreen(new Rect(0,0,v.getWidth(),v.getHeight()),true);});pause(300);
    }
    private void click(int id){show(id);ui(()->view(id).performClick());settle();}
    private void edit(int id,String value){ui(()->{
        EditText e=(EditText)view(id);int type=e.getInputType();
        e.setKeyListener(android.text.method.TextKeyListener.getInstance());e.setRawInputType(type);
        e.setText(value);
    });}
    private String text(int id){final String[] s={""};ui(()->s[0]=((TextView)view(id)).getText().toString());return s[0];}
    private String error(int id){final String[] s={""};ui(()->{CharSequence e=((TextView)view(id)).getError();s[0]=e==null?"":e.toString();});return s[0];}
    private void check(String label,boolean pass){
        if(!pass)throw new AssertionError(label);
        results.add("PASS | "+label);Bundle b=new Bundle();b.putString("stream","PASS | "+label+"\n");sendStatus(0,b);
    }
    private void shot(String name)throws Exception{
        hideKeyboard();pause(200);Bitmap b=getUiAutomation().takeScreenshot();
        if(b==null)throw new IOException("Screenshot unavailable");
        File dir=new File(getTargetContext().getExternalFilesDir(null),"evidence");dir.mkdirs();
        try(FileOutputStream out=new FileOutputStream(new File(dir,name+".png"))){b.compress(Bitmap.CompressFormat.PNG,100,out);}
    }
    private void save(String name,String data)throws Exception{
        File dir=new File(getTargetContext().getExternalFilesDir(null),"evidence");dir.mkdirs();
        try(FileOutputStream out=new FileOutputStream(new File(dir,name))){out.write(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    }
    private Activity waitActivity(String name){
        for(int i=0;i<100;i++){if(active!=null&&active.getClass().getSimpleName().equals(name)){settle();return active;}pause(100);}
        throw new AssertionError("Activity not resumed: "+name);
    }
    private void rotate(boolean landscape){
        Activity old=active;
        ui(()->old.setRequestedOrientation(landscape?ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));
        for(int i=0;i<80;i++){if(active!=old){settle();return;}pause(100);}
        throw new AssertionError("Rotation did not recreate Activity");
    }
    private void back(){sendKeyDownUpSync(KeyEvent.KEYCODE_BACK);settle();}
    private AccessibilityNodeInfo findNode(String id,String value){
        AccessibilityNodeInfo root=getUiAutomation().getRootInActiveWindow();if(root==null)return null;
        if(id!=null){List<AccessibilityNodeInfo> list=root.findAccessibilityNodeInfosByViewId(id);if(!list.isEmpty())return list.get(0);}
        if(value!=null){List<AccessibilityNodeInfo> list=root.findAccessibilityNodeInfosByText(value);for(AccessibilityNodeInfo node:list)if(value.equals(String.valueOf(node.getText()))||value.equals(String.valueOf(node.getContentDescription())))return node;}
        return null;
    }
    private void systemClick(String id,String text){
        for(int i=0;i<60;i++){AccessibilityNodeInfo node=findNode(id,text);if(node!=null){
            while(node!=null&&!node.isClickable())node=node.getParent();
            if(node!=null&&node.performAction(AccessibilityNodeInfo.ACTION_CLICK)){settle();return;}
        }pause(150);}
        throw new AssertionError("System control not found: "+id+" / "+text);
    }
    private void shell(String command)throws Exception{
        try(ParcelFileDescriptor fd=getUiAutomation().executeShellCommand(command);FileInputStream in=new FileInputStream(fd.getFileDescriptor())){byte[] b=new byte[2048];while(in.read(b)!=-1){}}
        settle();
    }
    private void execute()throws Exception{

        edit(R.id.edtSoA,"12");edit(R.id.edtSoB,"3");click(R.id.btnChia);
        check("A4-T01: 12 / 3 = 4.00",text(R.id.tvKetQua).endsWith("4.00"));shot("01_chia_dung");
        edit(R.id.edtSoB,"0");click(R.id.btnChia);
        check("A4-T02: divide by zero is rejected",error(R.id.edtSoB).contains("chia cho 0"));shot("02_loi_chia_0");
        edit(R.id.edtSoA,"");click(R.id.btnCong);
        check("A4-T03: empty A, error and focus",error(R.id.edtSoA).contains("trống")&&view(R.id.edtSoA).hasFocus());shot("03_loi_rong");
        edit(R.id.edtSoA,"abc");edit(R.id.edtSoB,"2");click(R.id.btnNhan);
        check("A4-T04: raw alphabetic input rejected without crash",error(R.id.edtSoA).contains("định dạng"));shot("04_loi_dinh_dang");
        edit(R.id.edtSoA,"-5.5");click(R.id.btnTru);
        check("A4-T05: -5.5 - 2 = -7.50",text(R.id.tvKetQua).endsWith("-7.50"));
        edit(R.id.edtSoA,"1.5.5");click(R.id.btnCong);
        check("A4-T06: multiple decimal points rejected",error(R.id.edtSoA).contains("định dạng"));
        click(R.id.btnXoa);
        edit(R.id.edtCanNang,"60");edit(R.id.edtChieuCao,"1.70");click(R.id.btnTinhBmi);
        check("A4-T07: 60kg / 1.70m = 20.8, normal",text(R.id.tvBmi).equals("BMI = 20.8")&&text(R.id.tvPhanLoai).equals("Bình thường"));shot("07_bmi_met");
        edit(R.id.edtChieuCao,"170");click(R.id.btnTinhBmi);
        check("A4-T08: centimetres give same BMI",text(R.id.tvBmi).equals("BMI = 20.8"));shot("08_bmi_cm");
        edit(R.id.edtChieuCao,"0");click(R.id.btnTinhBmi);
        check("A4-T09: zero height rejected, no Infinity",error(R.id.edtChieuCao).contains("lớn hơn 0")&&text(R.id.tvBmi).isEmpty());shot("09_loi_chieu_cao");
        edit(R.id.edtSoA,"1e309");click(R.id.btnCong);
        check("A4-T10: overflow input rejected",error(R.id.edtSoA).contains("định dạng"));
        check("A4-extra: addition/multiplication exact",Calculator.calculate(2,3,'+')==5&&Calculator.calculate(-2,3,'*')==-6);
        check("A4-extra: BMI boundaries 18.5/23/25",Calculator.bmiGroup(18.499)==0&&Calculator.bmiGroup(18.5)==1&&Calculator.bmiGroup(23)==2&&Calculator.bmiGroup(25)==3);
        for(String bad:new String[]{"NaN","Infinity","-","1,5",""}){
            boolean rejected=false;try{Calculator.parse(bad);}catch(NumberFormatException e){rejected=true;}
            check("A4-extra: rejects '"+bad+"'",rejected);
        }
        boolean overflow=false;try{Calculator.calculate(1e308,1e308,'*');}catch(IllegalArgumentException e){overflow=true;}
        check("A4-extra: result overflow rejected",overflow);
        for(int i=1;i<=6;i++){edit(R.id.edtSoA,""+i);edit(R.id.edtSoB,"2");click(R.id.btnCong);}
        String history=text(R.id.tvLichSu);check("A4-NC2: exactly 5 recent operations",history.split("\n").length==5&&history.startsWith("6.00"));
        rotate(true);check("A4-NC2: history survives actual rotation",text(R.id.tvLichSu).equals(history));show(R.id.tvLichSu);shot("NC2_lich_su_xoay");rotate(false);
        edit(R.id.edtCanNang,"80");edit(R.id.edtChieuCao,"170");click(R.id.btnTinhBmi);
        check("A4-NC3: obese label is red",((TextView)view(R.id.tvPhanLoai)).getCurrentTextColor()==0xffb3261e);shot("NC3_bmi_mau");
        click(R.id.btnXoa);check("A4-extra: clear resets operands, result, errors",text(R.id.edtSoA).isEmpty()&&text(R.id.edtSoB).isEmpty()&&error(R.id.edtSoA).isEmpty());

    }
}
