/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package psm;

import com.dtw.FastDTW;
import com.dtw.TimeWarpInfo;
import com.timeseries.TimeSeries;
import com.timeseries.TimeSeriesPoint;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author fiebrink
 */
public class RebeccaTestFastDTW {

    public void testSegmented(TimeSeries class1, TimeSeries class2, TimeSeries test1) {

            System.out.println("Class 1 is " + class1);
            System.out.println("Class1 num dimensions is " + class1.numOfDimensions());

            TimeWarpInfo info = com.dtw.FastDTW.getWarpInfoBetween(class1, test1, 5);

            System.out.println("Warp Distance: " + info.getDistance());
            System.out.println("Warp Path:     " + info.getPath());

            TimeWarpInfo info2 = com.dtw.FastDTW.getWarpInfoBetween(class2, test1, 5);

            System.out.println("Warp Distance2: " + info2.getDistance());
            System.out.println("Warp Path2:     " + info2.getPath());

        
    }

    public void testUnsegmented(TimeSeries class1, TimeSeries class2, TimeSeries test1) {
        //Test:
        System.out.println("times: ");
        for (int i = 0; i < test1.size(); i++) {
            System.out.print(test1.getTimeAtNthPoint(i) + "," );
            
        }
        System.out.println("");


        List<TimeSeries> windows = windowTimeseries(test1, 28);
        double min1 = Double.MAX_VALUE;
        
        double min2 = Double.MAX_VALUE;

        for (TimeSeries window : windows) {
            TimeWarpInfo info1 = FastDTW.getWarpInfoBetween(class1, window, 5);
            TimeWarpInfo info2 = FastDTW.getWarpInfoBetween(class2, window, 5);
            double dist1 = info1.getDistance();
            if (dist1 < min1)
                min1 = dist1;
            double dist2 = info2.getDistance();
            if (dist2 < min2)
                min2 = dist2;
        }

        System.out.println("Min dist 1 is " + min1 + ", min dist 2 is " + min2);
    }


    public List<TimeSeries> windowTimeseries(TimeSeries t, int windowSize) {
        List<TimeSeries> l = new LinkedList<TimeSeries>();
        //hop size = 1

        if (t.size() < windowSize) {
            l.add(new TimeSeries(t));
            return l;
        }

        int windowNum = 0;
        for (int startPos = 0; startPos <= t.size() - windowSize; startPos++) {
            TimeSeries tt = new TimeSeries(t.numOfDimensions());
            for (int i = 0; i < windowSize; i++) {
                double[] next = t.getMeasurementVector(startPos + i);
                tt.addLast(new Double(i), new TimeSeriesPoint(next));
            }
            l.add(tt);
            windowNum++;
        }

/*        System.out.println("Testing windowing: ");
        for (int i= 0; i < l.size(); i++) {
            System.out.println("Windowed # " + i + " is: " + l.get(i).toString());

        } */

        return l;
    }

    public void testSegmented(String file1, String file2, String file3) {
         TimeSeries ts1 = new TimeSeries(file1, false, false, ','); //class 1
         TimeSeries ts2 = new TimeSeries(file2, false, false, ','); // class 2
         TimeSeries ts3 = new TimeSeries(file3, false, false, ','); //test input
         testSegmented(ts1, ts2, ts3);

    }

    public void testUnsegmented(String file1, String file2, String file3) {
        TimeSeries ts1 = new TimeSeries(file1, false, false, ','); //class 1
         TimeSeries ts2 = new TimeSeries(file2, false, false, ','); // class 2
         TimeSeries ts3 = new TimeSeries(file3, false, false, ','); //test input
         testUnsegmented(ts1, ts2, ts3);
        
    }


    public static void main(String[] args)
      {
        RebeccaTestFastDTW t = new RebeccaTestFastDTW();
      //  t.testUnsegmented("/Users/rebecca/tmp1.csv", "/Users/rebecca/tmp2.csv", "/Users/rebecca/tmp5.csv");
        t.testSegmented("/Users/rebecca/tmp1.csv", "/Users/rebecca/tmp2.csv", "/Users/rebecca/tmp5.csv");

      }  // end main()

}
