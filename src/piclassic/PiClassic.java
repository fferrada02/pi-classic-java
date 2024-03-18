package piclassic;

/*  copyright Pascal Sebah : September 1999
 ** adapted to java by : Freddy Ferrada Matamala 2024
 **
 ** Subject:
 **
 **    A very easy program to compute Pi with many digits.
 **    No optimisations, no tricks, just a basic program to learn how
 **    to compute in multiprecision.
 **
 ** Formulae:
 **
 **    Pi/4 =    arctan(1/2)+arctan(1/3)                     (Hutton 1)
 **    Pi/4 =  2*arctan(1/3)+arctan(1/7)                     (Hutton 2)
 **    Pi/4 =  4*arctan(1/5)-arctan(1/239)                   (Machin)
 **    Pi/4 = 12*arctan(1/18)+8*arctan(1/57)-5*arctan(1/239) (Gauss)
 **
 **      with arctan(x) =  x - x^3/3 + x^5/5 - ...
 **
 **    The Lehmer's measure is the sum of the inverse of the decimal
 **    logarithm of the pk in the arctan(1/pk). The more the measure
 **    is small, the more the formula is efficient.
 **    For example, with Machin's formula:
 **
 **      E = 1/log10(5)+1/log10(239) = 1.852
 **
 ** Data:
 **
 **    A big real (or multiprecision real) is defined in base B as:
 **      X = x(0) + x(1)/B^1 + ... + x(n-1)/B^(n-1)
 **      where 0<=x(i)<B
 **
 ** Results: (PentiumII, 450Mhz)
 **
 **   Formula      :    Hutton 1  Hutton 2   Machin   Gauss
 **   Lehmer's measure:   5.418     3.280      1.852    1.786
 **
 **  1000   decimals:     0.2s      0.1s       0.06s    0.06s
 **  10000  decimals:    19.0s     11.4s       6.7s     6.4s
 **  100000 decimals:  1891.0s   1144.0s     785.0s   622.0s
 **
 ** With a little work it's possible to reduce those computation
 ** times by a factor 3 and more:
 **
 **     => Work with double instead of long and the base B can
 **        be choosen as 10^8
 **     => During the iterations the numbers you add are smaller
 **        and smaller, take this in account in the +, *, /
 **     => In the division of y=x/d, you may precompute 1/d and
 **        avoid multiplications in the loop (only with doubles)
 **     => MaxDiv may be increased to more than 3000 with doubles
 **     => ...
 */


public class PiClassic {
    public static int B = 10000; /* Working base */
    public static int LB = 4;    /* Log10(base)  */
    public static int MaxDiv=450;  /* about sqrt(2^31/B) */

    /*
     ** Computation of the constant Pi with arctan relations
     */
    public static void main(String[] args)  {
        int NbDigits=10000, NbArctan;
        int[] p = new int[3]; int[] m = new int[3];
        int size=1+NbDigits/PiClassic.LB; int  i;
        long[] Pi     = new long[size];
        long[] arctan  = new long[size];
        long[] buffer1 = new long[size];
        long[] buffer2 = new long[size];
        /*
         ** Formula used:
         **
         **   Pi/4 = 12*arctan(1/18)+8*arctan(1/57)-5*arctan(1/239) (Gauss)
         */
        NbArctan = 3;
        m[0] = 12; m[1] = 8;  m[2] = -5;
        p[0] = 18; p[1] = 57; p[2] = 239;
        PiClassic.SetToInteger(size, Pi, 0);
        /*
         ** Computation of Pi/4 = Sum(i) [m[i]*arctan(1/p[i])]
         */
        for (i=0; i<NbArctan; i++) {
            PiClassic.arccot(p[i], size, arctan, buffer1, buffer2);
            PiClassic.Mul(size, arctan, m[i]>0?m[i]:-m[i]);
            if (m[i]>0) PiClassic.Add(size, Pi, arctan);
            else        PiClassic.Sub(size, Pi, arctan);
        }
        PiClassic.Mul(size, Pi, 4);
        System.out.println("Pi/4 = 12*arctan(1/18)+8*arctan(1/57)-5*arctan(1/239) (Gauss)\n");
        PiClassic.Print(size, Pi);  /* Print out of Pi */
    }


    /*
     ** Set the big real x to the small integer Integer
     */
    public static void SetToInteger (int n, long[] x, int  entero) {
        for (int i=1; i<n; i++) x[i] = 0;
        x[0] = entero;
    }
    /*
     ** Is the big real x equal to zero ?
     */
    public static long IsZero (int n, long[] x) {

        int i;
        for (i=0; i<n; i++)
            if (x[i] != 0)
                return 0;

        return 1;
    }
    /*
     ** Addition of big reals : x += y
     **  Like school addition with carry management
     */
    public static void Add (int n, long[] x, long[] y) {
        long carry=0;
        int i;
        for (i=n-1; i>=0; i--) {
            x[i] += y[i]+carry;
            if (x[i]<PiClassic.B) carry = 0;
            else {
                carry = 1;
                x[i] -= PiClassic.B;
            }
        }
    }
    /*
     ** Substraction of big reals : x -= y
     **  Like school substraction with carry management
     **  x must be greater than y
     */
    public static void Sub (int n, long[] x, long[] y) {
        int i;
        for (i=n-1; i>=0; i--) {
            x[i] -= y[i];
            if (x[i]<0) {
                if (i > 0) {
                    x[i] += PiClassic.B;
                    x[i-1]--;
                }
            }
        }
    }
    /*
     ** Multiplication of the big real x by the integer q
     ** x = x*q.
     **  Like school multiplication with carry management
     */
    public static void Mul (int n, long[] x, int q) {
        long carry=0;
        long xi;
        int i;
        for (i=n-1; i>=0; i--) {
            xi  = x[i]*q;
            xi += carry;
            if (xi>=PiClassic.B) {
                carry = xi/PiClassic.B;
                xi -= (carry*PiClassic.B);
            }
            else
                carry = 0;
            x[i] = xi;
        }
    }
    /*
     ** Division of the big real x by the integer d
     ** The result is y=x/d.
     **  Like school division with carry management
     **  d is limited to MaxDiv*MaxDiv.
     */
    public static void Div(int n, long[] x, int d, long[] y) {
        long carry=0;
        long xi;
        long q;
        int i;
        for (i=0; i<n; i++) {
            xi    = x[i]+carry*PiClassic.B;
            q     = xi/d;
            carry = xi-q*d;
            y[i]  = q;
        }
    }
    /*
     ** Find the arc cotangent of the integer p = arctan (1/p)
     **  Result in the big real x (size n)
     **  buf1 and buf2 are two buffers of size n
     */
    public static void arccot (int p, int n, long[] x, long[] buf1, long[] buf2) {
        int p2=p*p, k=3, sign=0;
        long[] uk = buf1; long[]  vk=buf2;
        PiClassic.SetToInteger(n, x, 0);
        PiClassic.SetToInteger(n, uk, 1);	/* uk = 1/p */
        PiClassic.Div(n, uk, p, uk);
        PiClassic.Add(n, x, uk);	          /* x  = uk */

        while ( PiClassic.IsZero(n, uk) == 0) {
            if (p<PiClassic.MaxDiv)
                PiClassic.Div(n, uk, p2, uk);  /* One step for small p */
            else {
                PiClassic.Div(n, uk, p, uk);   /* Two steps for large p (see division) */
                PiClassic.Div(n, uk, p, uk);
            }
            /* uk = u(k-1)/(p^2) */
            PiClassic.Div(n, uk, k, vk);       /* vk = uk/k  */
            if (sign == 1) PiClassic.Add(n, x, vk); /* x = x+vk   */
            else PiClassic.Sub(n, x, vk);      /* x = x-vk   */
            k+=2;
            sign = 1-sign;
        }
    }
    /*
     ** Print the big real x
     */
    public static void Print (long n, long[] x) {
        int i;
        System.out.printf("%d.\n", x[0]);
        for (i=1; i<n; i++) {
            System.out.printf("%04d", x[i]);
            if (i % 25 == 0) System.out.printf("% 16d\n", i*4);
        }
        System.out.printf("\n");
    }
}
