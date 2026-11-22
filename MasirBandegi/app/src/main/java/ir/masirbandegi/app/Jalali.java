package ir.masirbandegi.app;
import java.time.LocalDate;

public final class Jalali {
  private Jalali() {}
  public static String format(LocalDate date) {
    int gy=date.getYear(), gm=date.getMonthValue(), gd=date.getDayOfMonth();
    int[] gdm={0,31,59,90,120,151,181,212,243,273,304,334};
    int gy2=(gm>2)?gy+1:gy;
    int days=355666+365*gy+(gy2+3)/4-(gy2+99)/100+(gy2+399)/400+gd+gdm[gm-1];
    int jy=-1595+33*(days/12053); days%=12053;
    jy+=4*(days/1461); days%=1461;
    if(days>365){jy+=(days-1)/365; days=(days-1)%365;}
    int jm, jd;
    if(days<186){jm=1+days/31; jd=1+days%31;} else {jm=7+(days-186)/30; jd=1+(days-186)%30;}
    return String.format(java.util.Locale.forLanguageTag("fa-IR"), "%04d/%02d/%02d", jy,jm,jd);
  }
}
