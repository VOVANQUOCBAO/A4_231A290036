package vn.edu.vhu.ltdd.a4events;


import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends LabActivity {
    private EditText edtSoA,edtSoB,edtCanNang,edtChieuCao;
    private TextView tvKetQua,tvBmi,tvPhanLoai,tvLichSu;
    private ArrayList<String> history=new ArrayList<>();
    private double lastBmi=-1;
    private Toast activeToast;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        showLayout(R.layout.activity_main);
        edtSoA=findViewById(R.id.edtSoA); edtSoB=findViewById(R.id.edtSoB);
        edtCanNang=findViewById(R.id.edtCanNang); edtChieuCao=findViewById(R.id.edtChieuCao);
        tvKetQua=findViewById(R.id.tvKetQua); tvBmi=findViewById(R.id.tvBmi);
        tvPhanLoai=findViewById(R.id.tvPhanLoai); tvLichSu=findViewById(R.id.tvLichSu);
        // Cách 1: listener riêng bằng lambda.
        findViewById(R.id.btnCong).setOnClickListener(v->tinhToan('+'));
        findViewById(R.id.btnTru).setOnClickListener(v->tinhToan('-'));
        // Cách 2: listener chung; R.id không dùng trong switch.
        View.OnClickListener shared=v->{
            if(v.getId()==R.id.btnNhan) tinhToan('*');
            else if(v.getId()==R.id.btnChia) tinhToan('/');
        };
        findViewById(R.id.btnNhan).setOnClickListener(shared);
        findViewById(R.id.btnChia).setOnClickListener(shared);
        findViewById(R.id.btnXoa).setOnClickListener(v->xoaTrang());
        findViewById(R.id.btnTinhBmi).setOnClickListener(v->tinhBmi());
        if(state!=null) {
            ArrayList<String> saved=state.getStringArrayList("history");
            if(saved!=null) history=saved;
            tvKetQua.setText(state.getString("result",getString(R.string.result_placeholder)));
            lastBmi=state.getDouble("bmi",-1);
            if(lastBmi>0) showBmi(lastBmi);
        }
        showHistory();
    }
    private void error(EditText field,String text) {
        field.setError(text); field.requestFocus();
        clearNotice();
        activeToast=Toast.makeText(this,text,Toast.LENGTH_SHORT); activeToast.show();
    }
    private void clearNotice() {
        if(activeToast!=null) {activeToast.cancel(); activeToast=null;}
    }
    private Double readNumber(EditText field) {
        field.setError(null);
        String raw=field.getText().toString().trim();
        if(raw.isEmpty()) { error(field,getString(R.string.err_empty)); return null; }
        try { return Calculator.parse(raw); }
        catch(NumberFormatException e) {
            error(field,getString(R.string.err_not_number)); Log.w(TAG,"Dữ liệu số không hợp lệ",e); return null;
        }
    }
    private void tinhToan(char op) {
        clearNotice();
        tvKetQua.setText(R.string.result_placeholder);
        edtSoA.setError(null); edtSoB.setError(null);
        Double a=readNumber(edtSoA); if(a==null) return;
        Double b=readNumber(edtSoB); if(b==null) return;
        if(op=='/'&&b==0) { error(edtSoB,getString(R.string.err_divide_zero)); return; }
        try {
            double result=Calculator.calculate(a,b,op);
            String text=String.format(Locale.getDefault(),"%.2f %c %.2f = %.2f",a,op,b,result);
            tvKetQua.setText(text);
            history.add(0,text); if(history.size()>5) history.remove(5);
            showHistory(); Log.d(TAG,"Phép tính: "+text);
        } catch(IllegalArgumentException e) { error(edtSoA,e.getMessage()); }
    }
    private void xoaTrang() {
        clearNotice();
        edtSoA.setText(""); edtSoB.setText("");
        edtSoA.setError(null); edtSoB.setError(null);
        tvKetQua.setText(R.string.result_placeholder); edtSoA.requestFocus();
    }
    private void showHistory() {
        tvLichSu.setText(history.isEmpty()?getString(R.string.history_empty):android.text.TextUtils.join("\n",history));
    }
    private void tinhBmi() {
        clearNotice();
        lastBmi=-1; tvBmi.setText(""); tvPhanLoai.setText("");
        edtCanNang.setError(null); edtChieuCao.setError(null);
        Double weight=readNumber(edtCanNang); if(weight==null) return;
        Double height=readNumber(edtChieuCao); if(height==null) return;
        if(weight<=0) { error(edtCanNang,getString(R.string.err_positive)); return; }
        if(height<=0) { error(edtChieuCao,getString(R.string.err_positive)); return; }
        try { lastBmi=Calculator.bmi(weight,height); showBmi(lastBmi); }
        catch(IllegalArgumentException e) { error(edtChieuCao,e.getMessage()); }
    }
    private void showBmi(double bmi) {
        int group=Calculator.bmiGroup(bmi);
        int[] names={R.string.bmi_under,R.string.bmi_normal,R.string.bmi_over,R.string.bmi_obese};
        int[] colors={0xff1565c0,0xff147d35,0xff9a6700,0xffb3261e};
        tvBmi.setText(String.format(Locale.getDefault(),"BMI = %.1f",bmi));
        tvPhanLoai.setText(names[group]); tvPhanLoai.setTextColor(colors[group]);
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putStringArrayList("history",history);
        state.putString("result",tvKetQua.getText().toString()); state.putDouble("bmi",lastBmi);
        super.onSaveInstanceState(state);
    }
}
