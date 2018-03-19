package de.embl.cba.trainableDeepSegmentation.results;

import de.embl.cba.bigDataTools.utils.Utils;
import fiji.util.gui.GenericDialogPlus;
import ij.ImagePlus;
import ij.gui.GenericDialog;

import java.util.ArrayList;

import static de.embl.cba.trainableDeepSegmentation.results.ResultUtils.*;

public abstract class ResultImageGUI {

    public static void showExportGUI( String inputImageTitle,
                                      ResultImage resultImage,
                                      ImagePlus rawData,
                                      ArrayList< String > classNames )
    {

        String[] exportChoices = new String[]{ SHOW_AS_SEPARATE_IMAGES, SEPARATE_IMARIS, SEPARATE_TIFF_FILES };

        GenericDialog gd = new GenericDialogPlus("Export Segmentation Results");

        gd.addStringField( "Class names prefix: ", inputImageTitle + "--", 10  );

        gd.addStringField( "Binning: ", "1,1,1", 10  );

        gd.addMessage( "--- Export ---" );

        gd.addCheckbox( rawData.getTitle(), true );

        for ( String className : classNames )
        {
            gd.addCheckbox( className, true );
        }


        gd.addMessage( "--- Spatial proximity filtering (currently not working for movies) ---" );

        gd.addCheckbox( "Do spatial proximity filtering", false );
        gd.addNumericField( "Distance (after binning) [pixels]:", 10, 0 );

        gd.addChoice( "Reference class:", classNames.toArray( new String[classNames.size()] ), classNames.get( 0 ) );

        gd.addMessage( "--- Export modality ---" );

        gd.addChoice( "Export as:", exportChoices, ResultUtils.SEPARATE_IMARIS );

        gd.showDialog();

        if ( gd.wasCanceled() ) return;

        ResultExportSettings resultExportSettings = new ResultExportSettings();
        resultExportSettings.classNames = classNames;
        resultExportSettings.resultImage = resultImage;
        resultExportSettings.rawData = rawData;

        setFromGUI( classNames, gd, resultExportSettings );

        resultImage.exportResults( resultExportSettings );

    }

    private static void setFromGUI( ArrayList< String > classNames, GenericDialog gd, ResultExportSettings resultExportSettings )
    {
        resultExportSettings.exportNamesPrefix = gd.getNextString();

        resultExportSettings.binning = Utils.delimitedStringToIntegerArray( gd.getNextString().trim(), ",");

        setExport( classNames, gd, resultExportSettings );

        setSpatialProximityFiltering( gd, resultExportSettings );

        resultExportSettings.exportType = gd.getNextChoice();
    }

    private static void setExport( ArrayList< String > classNames, GenericDialog gd, ResultExportSettings resultExportSettings )
    {
        resultExportSettings.saveRawData = gd.getNextBoolean();

        resultExportSettings.classesToBeExported = new ArrayList<>();

        for ( String className : classNames ) resultExportSettings.classesToBeExported.add( gd.getNextBoolean() );
    }

    private static void setSpatialProximityFiltering( GenericDialog gd, ResultExportSettings resultExportSettings )
    {
        resultExportSettings.proximityFilterSettings = new ProximityFilterSettings();
        resultExportSettings.proximityFilterSettings.doSpatialProximityFiltering = gd.getNextBoolean();
        resultExportSettings.proximityFilterSettings.distanceInPixelsAfterBinning = (int) gd.getNextNumber();
        resultExportSettings.proximityFilterSettings.referenceClassId = (int) gd.getNextChoiceIndex();
    }


}
