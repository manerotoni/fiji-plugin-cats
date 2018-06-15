package de.embl.cba.trainableDeepSegmentation;

import bdv.spimdata.SpimDataMinimal;
import de.embl.cba.trainableDeepSegmentation.results.ResultExportSettings;
import de.embl.cba.trainableDeepSegmentation.utils.IntervalUtils;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import net.imglib2.FinalInterval;

import bdv.util.BdvFunctions;

import bdv.img.imaris.*;

import java.io.IOException;

import static de.embl.cba.trainableDeepSegmentation.TestUtils.TEST_RESOURCES;

public class TestResultsExport
{
    public static void main( final String[] args )
    {

        new ImageJ();

//        fibSemCell();

        objects3d();



    }

    private static void objects3d()
    {
        // Open Image
        //
        ImagePlus inputImagePlus = IJ.openImage(TEST_RESOURCES + "3d-objects-2channels.zip" );
//        ImagePlus inputImagePlus = IJ.openImage(TEST_RESOURCES + "3d-objects-2channels.zip" );

        inputImagePlus.show();

        DeepSegmentation deepSegmentation = new DeepSegmentation();
        deepSegmentation.setInputImage( inputImagePlus );
        deepSegmentation.setResultImageDisk( TEST_RESOURCES + "3d-objects-probabilities" );
        deepSegmentation.loadInstancesAndMetadata( TEST_RESOURCES + "3d-objects-instances/3d-objects.ARFF"  );

        ResultExportSettings resultExportSettings = new ResultExportSettings();
        resultExportSettings.directory = TEST_RESOURCES + "3d-objects-export";
        resultExportSettings.exportType = ResultExportSettings.SEPARATE_IMARIS;
        resultExportSettings.classNames = deepSegmentation.getClassNames();
        resultExportSettings.timePointsFirstLast = new int[]{ 0, 0 };
        resultExportSettings.saveRawData = true;
        resultExportSettings.inputImagePlus = inputImagePlus;

        deepSegmentation.getResultImage().exportResults( resultExportSettings );

        try
        {
            SpimDataMinimal spimData = Imaris.openIms( TEST_RESOURCES + "3d-objects-export/meta.ims" );
            BdvFunctions.show( spimData );
        } catch ( IOException e )
        {
            e.printStackTrace();
        }
    }

    private static void fibSemCell()
    {
        // Open Image
        //
        ImagePlus imp = IJ.openImage( "/Users/tischer/Documents/fiji-plugin-deepSegmentation/src/test/resources/fib-sem--cell--8x8x8nm--nt2.zip" );

        FinalInterval fullImageInterval = IntervalUtils.getInterval( imp );
        long[] min = new long[ 5 ];
        long[] max = new long[ 5 ];
        fullImageInterval.min( min );
        fullImageInterval.max( max );
        //max[ X ] = 50;
        //max[ Y ] = 50;
        //min[ Z ] = 2; max[ Z ] = 5;
        FinalInterval interval = new FinalInterval( min, max );

        DeepSegmentation deepSegmentation = new DeepSegmentation();
        deepSegmentation.setInputImage( imp );
        deepSegmentation.setResultImageRAM( interval );
        deepSegmentation.loadInstancesAndMetadata( "/Users/tischer/Documents/fiji-plugin-deepSegmentation/src/test/resources/fib-sem--cell--8x8x8nm.ARFF" );

        deepSegmentation.classifierNumTrees = 10;
        deepSegmentation.trainClassifier( "fib-sem--cell--8x8x8nm.tif" );
        deepSegmentation.applyClassifierWithTiling( interval );

        deepSegmentation.getInputImage().show();
        deepSegmentation.getResultImage().getWholeImageCopy().show();

        ResultExportSettings resultExportSettings = new ResultExportSettings();
        resultExportSettings.directory = "/Users/tischer/Desktop/tmp4";
        resultExportSettings.exportType = ResultExportSettings.SEPARATE_IMARIS;
        resultExportSettings.classNames = deepSegmentation.getClassNames();
        resultExportSettings.timePointsFirstLast = new int[]{ 0, 1 };
        resultExportSettings.saveRawData = true;
        resultExportSettings.inputImagePlus = imp;

        deepSegmentation.getResultImage().exportResults( resultExportSettings );

        try
		{
			SpimDataMinimal spimData = Imaris.openIms( "/Users/tischer/Desktop/tmp4/meta.ims" );
			BdvFunctions.show( spimData );
		} catch ( IOException e )
		{
			e.printStackTrace();
		}
    }

}
