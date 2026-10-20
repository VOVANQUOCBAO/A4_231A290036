package vn.edu.vhu.ltdd.a4events;


public final class Calculator {
    private Calculator() { }
    public static double parse(String value) {
        String s=value.trim();
        if (!s.matches("[+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:[eE][+-]?[0-9]+)?"))
            throw new NumberFormatException("Sai định dạng số");
        double number=Double.parseDouble(s);
        if (!Double.isFinite(number)) throw new NumberFormatException("Số vượt giới hạn");
        return number;
    }
    public static double calculate(double a,double b,char op) {
        if (!Double.isFinite(a)||!Double.isFinite(b)) throw new IllegalArgumentException("Số không hữu hạn");
        if (op=='/' && b==0) throw new IllegalArgumentException("Không thể chia cho 0");
        double result;
        switch(op) {
            case '+': result=a+b; break;
            case '-': result=a-b; break;
            case '*': result=a*b; break;
            case '/': result=a/b; break;
            default: throw new IllegalArgumentException("Phép toán không hợp lệ");
        }
        if (!Double.isFinite(result)) throw new IllegalArgumentException("Kết quả vượt giới hạn số");
        return result;
    }
    public static double bmi(double weight,double height) {
        if (!Double.isFinite(weight)||!Double.isFinite(height)||weight<=0||height<=0)
            throw new IllegalArgumentException("Cân nặng và chiều cao phải lớn hơn 0");
        double meters=height>3?height/100.0:height;
        double result=weight/(meters*meters);
        if (!Double.isFinite(result)||result<=0) throw new IllegalArgumentException("BMI vượt giới hạn số");
        return result;
    }
    public static int bmiGroup(double value) {
        if(value<18.5) return 0;
        if(value<23) return 1;
        if(value<25) return 2;
        return 3;
    }
}
