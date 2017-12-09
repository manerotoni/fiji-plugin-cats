package trainableDeepSegmentation;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import net.imglib2.FinalInterval;
import net.imglib2.util.Intervals;

import java.util.ArrayList;

import static trainableDeepSegmentation.IntervalUtils.*;

public class TestTrainLabelImageHeadless {

    final static String ROOT_PATH = "/Users/tischi/Documents/segmentation-challenges--data/brainiac/";

    final static String LOGGING_DIRECTORY = ROOT_PATH + "log";
    final static String INPUT_IMAGE_PATH = ROOT_PATH + "combined-clahe-crop-nz16.tif";
    final static String LABEL_IMAGE_PATH = ROOT_PATH + "train-labels-border2-crop-nz16.tif";
    final static String INSTANCES_DIRECTORY = ROOT_PATH + "instances";

    public static void main( final String[] args )
    {
        new ImageJ();

        // Load image data
        //
        ImagePlus inputImage = IJ.openImage( INPUT_IMAGE_PATH );
        ImagePlus labelImage = IJ.openImage( LABEL_IMAGE_PATH );
        IJ.run(labelImage, "Divide...", "value=255.0000 stack");

        // Set up weka
        //
        WekaSegmentation ws = new WekaSegmentation( );
        ws.setNumThreads( 4 );

        ws.setInputImage( inputImage );
        ws.setLabelImage( labelImage );
        ws.setResultImageRAM( );

        ws.setAndCreateLogDirAbsolute( LOGGING_DIRECTORY );

        ws.settings.log2 = false;
        ws.settings.binFactors[0] = 2;
        ws.settings.binFactors[1] = -1;
        ws.settings.binFactors[2] = -1;
        ws.settings.binFactors[3] = -1;
        ws.settings.anisotropy = 5;
        ws.settings.activeChannels.add( 0 );

        ws.classifierBatchSizePercent = "100";
        ws.classifierNumTrees = 50;
        ws.classifierFractionFeaturesPerNode = 0.1;

        //FinalInterval interval = IntervalUtils.getInterval( inputImage );
        //IntervalUtils.getIntervalByReplacingValues(
        //       interval, IntervalUtils.Z, 1, 96 );


        long[] min = new long[ 5 ];
        long[] max = new long[ 5 ];
        min[ X ] = 0; max[ X ] = inputImage.getWidth() - 1;
        min[ Y ] = 0; max[ Y ] = inputImage.getHeight() - 1;
        min[ Z ] = 0; max[ Z ] = 15;
        min[ C ] = 0; max[ C ] = 0;
        min[ T ] = 0; max[ T ] = 0;
        FinalInterval trainInterval = new FinalInterval( min, max );


        long[] min2 = Intervals.minAsLongArray( trainInterval );
        long[] max2 = Intervals.maxAsLongArray( trainInterval );
        min2[ T ] = 0; max2[ T ] = 1;
        FinalInterval applyInterval = new FinalInterval( min2, max2 );


        ArrayList< Double > classWeights = new ArrayList<>(  );
        classWeights.add( 1.0 );
        classWeights.add( 1.0 );

        ws.trainFromLabelImage(
                "labelImageTraining",
                WekaSegmentation.START_NEW_INSTANCES,
                1,
                4,
                1,
                7,
               20,
                2000,
                100,
                classWeights,
                INSTANCES_DIRECTORY,
                trainInterval,
                applyInterval);


        ws.getInputImage().show();
        ws.getLabelImage().show();

        // Analyse objects
        //
        //ImagePlus labelMask = ws.computeClassLabelMask( MIN_VOXELS );
        //labelMask.show();

        // Save image
        //String outFilePath = OUTPUT_DIR + File.separator + "R"+ regExp + "--labelMask.tif";
        //WekaSegmentation.logger.info("\n# Saving file " + outFilePath + "...");
        //FileSaver fileSaver = new FileSaver( labelMask );
        //fileSaver.saveAsTiff( outFilePath );
        //WekaSegmentation.logger.info("...done.");


    }
}
