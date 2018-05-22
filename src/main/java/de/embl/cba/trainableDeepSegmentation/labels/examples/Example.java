package de.embl.cba.trainableDeepSegmentation.labels.examples;

import java.awt.*;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by de.embl.cba.trainableDeepSegmentation.weka on 29/05/17.
 */
public class Example implements Serializable {

    public int classNum;
    public Point[] points = null;
    public int strokeWidth = 0;
    public int z; // zero-based
    public int t; // zero-based
    public ArrayList< ArrayList< double[] > > instanceValuesArrays = null;
    public boolean instanceValuesAreCurrentlyBeingComputed = false;

    public Example( int classNum, Point[] points, int strokeWidth, int z, int t )
    {
        this.classNum = classNum;
        this.points = points;
        this.strokeWidth = strokeWidth;
        this.t = t;
        this.z = z;
    }

}