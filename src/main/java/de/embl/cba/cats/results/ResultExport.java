package de.embl.cba.cats.results;

import de.embl.cba.cats.postprocessing.ProximityFilter3D;
import de.embl.cba.cats.utils.IOUtils;
import de.embl.cba.imaris.H5DataCubeWriter;
import de.embl.cba.imaris.ImarisDataSet;
import de.embl.cba.imaris.ImarisUtils;
import de.embl.cba.imaris.ImarisWriter;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.Prefs;
import ij.plugin.Binner;
import ij.plugin.Duplicator;
import ij.process.ImageProcessor;
import net.imglib2.FinalInterval;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static de.embl.cba.bigdataprocessor.utils.Utils.applyIntensityGate;
import static de.embl.cba.cats.CATS.logger;
import static de.embl.cba.cats.utils.IntervalUtils.*;

public abstract class ResultExport
{

    private static void saveClassAsImaris( int classId, ResultExportSettings resultExportSettings )
    {
        String fileName = resultExportSettings.classNames.get( classId );

        ImarisDataSet imarisDataSet = new ImarisDataSet(
                resultExportSettings.resultImagePlus,
                resultExportSettings.binning,
                resultExportSettings.directory,
                fileName );

        setChannelName( fileName, imarisDataSet );

        ImarisWriter.writeHeaderFile( imarisDataSet, resultExportSettings.directory, resultExportSettings.exportNamesPrefix + fileName + ".ims" );

        H5DataCubeWriter writer = new H5DataCubeWriter();

        for ( int t = resultExportSettings.timePointsFirstLast[ 0 ]; t <= resultExportSettings.timePointsFirstLast[ 1 ]; ++t )
        {
            ImagePlus impClass = getBinnedAndProximityFilteredClassImage( classId, resultExportSettings, t );

            logger.progress( "Writing " + fileName+ ", frame:", ( t + 1 ) + "/" + resultExportSettings.resultImagePlus.getNFrames() + "..." );

            writer.writeImarisCompatibleResolutionPyramid( impClass, imarisDataSet, 0, t );
        }
    }


    public static void saveRawDataAsImaris( ResultExportSettings resultExportSettings  )
    {

        String fileName = "raw-data";

        ImarisDataSet imarisDataSet = new ImarisDataSet(
                resultExportSettings.inputImagePlus,
                resultExportSettings.binning,
                resultExportSettings.directory,
                fileName );

        // Header
        ImarisWriter.writeHeaderFile( imarisDataSet, resultExportSettings.directory, resultExportSettings.exportNamesPrefix + fileName + ".ims" );

        H5DataCubeWriter writer = new H5DataCubeWriter();

        for ( int c = 0; c < imarisDataSet.getNumChannels(); ++c )
        {
            for ( int t = resultExportSettings.timePointsFirstLast[ 0 ]; t <= resultExportSettings.timePointsFirstLast[ 1 ]; ++t )
            {

                logger.progress( "Writing " + fileName,
                        ", frame: " + ( t + 1 ) + "/" + resultExportSettings.resultImagePlus.getNFrames()
                                + ", channel: "+ ( c + 1 ) + "/" + imarisDataSet.getNumChannels() + "..."
                );

                //logger.info( "Copying into RAM..." );
                ImagePlus rawDataFrame = getBinnedRawDataFrame( resultExportSettings, c, t );

                //logger.info( "Writing as Imaris..." );
                writer.writeImarisCompatibleResolutionPyramid( rawDataFrame, imarisDataSet, c, t );
            }
        }


    }


    private ImagePlus getChannelView( final ImagePlus imp, final int channel )
    {
//        final int imagePlusChannelDimension = 2;
//        RandomAccessibleInterval rai = ImageJFunctions.wrap( imp );
//        final IntervalView singleChannelView = Views.hyperSlice( rai, imagePlusChannelDimension, channel );
        return null;
    }

    private static void setChannelName( String fileName, ImarisDataSet imarisDataSet )
    {
        ArrayList< String > channelNames = new ArrayList<>();

        channelNames.add( fileName );

        imarisDataSet.setChannelNames( channelNames );
    }


    private static void logDone( ResultExportSettings resultExportSettings,
                                 String className,
                                 int t,
                                 String s )
    {
        logger.progress( s + className + ", frame:",
                ( t + 1 ) + "/" + resultExportSettings.resultImagePlus.getNFrames() );
    }

    public static ImagePlus getBinnedClassImage( int classId, ResultExportSettings resultExportSettings, int t )
    {

        ImagePlus impClass = getClassImage( classId, t, resultExportSettings );

        if ( resultExportSettings.binning[0] * resultExportSettings.binning[1] * resultExportSettings.binning[2] > 1 )
        {
            Binner binner = new Binner();
            impClass = binner.shrink( impClass, resultExportSettings.binning[ 0 ], resultExportSettings.binning[ 1 ], resultExportSettings.binning[ 2 ], Binner.AVERAGE );
        }

        return impClass;
    }


    public static ImagePlus getBinnedClassImageMemoryEfficient(
            int classId, ResultExportSettings resultExportSettings, int t,
            de.embl.cba.utils.logging.Logger logger, int numThreads )
    {

        logger.info( "\nComputing probability image, using " + numThreads + " threads." );

        int nz = (int) resultExportSettings.resultImage.getDimensions()[ Z ];
        int nx = (int) resultExportSettings.resultImage.getDimensions()[ X ];
        int ny = (int) resultExportSettings.resultImage.getDimensions()[ Y ];

        int dx = resultExportSettings.binning[0];
        int dy = resultExportSettings.binning[1];
        int dz = resultExportSettings.binning[2];

        ImageStack binnedStack =
                new ImageStack(
                        nx / dx,
                        ny / dy,
                        (int) Math.ceil( 1.0 * nz / dz ) );

        long startTime = System.currentTimeMillis();

        ExecutorService exe = Executors.newFixedThreadPool( numThreads );
        ArrayList< Future< ImagePlus > > futures = new ArrayList<>(  );

        for ( int iz = 0; iz < nz; iz += dz )
        {
            futures.add(
                    exe.submit(
                            CallableResultImageBinner.getBinned(
                                    resultExportSettings,
                                    classId,
                                    iz, iz + dz - 1, t,
                                    logger,
                                    startTime,
                                    nz )
                    )
            );
        }


        int i = 0;
        for ( Future<ImagePlus> future : futures )
        {
            // getInstancesAndMetadata feature images
            try
            {
                ImagePlus binnedSlice = future.get();
                binnedStack.setProcessor( binnedSlice.getProcessor(), ++i );
                System.gc();
            }
            catch ( InterruptedException e )
            {
                e.printStackTrace();
            }
            catch ( ExecutionException e )
            {
                e.printStackTrace();
            }
        }

        futures = null;
        exe.shutdown();
        System.gc();

        ImagePlus binnedClassImage =
                new ImagePlus( "binned_class_" + classId, binnedStack );

        return binnedClassImage;
    }


    public static ImagePlus getBinnedAndProximityFilteredClassImage( int classId, ResultExportSettings resultExportSettings, int t )
    {
        ImagePlus impClass = getBinnedClassImageMemoryEfficient( classId, resultExportSettings, t, logger, Prefs.getThreads() );

        if ( resultExportSettings.proximityFilterSettings.doSpatialProximityFiltering )
        {
            logger.info( "Applying proximity filter..." );
            impClass = ProximityFilter3D.multiply( impClass, resultExportSettings.proximityFilterSettings.dilatedBinaryReferenceMask );
        }

        return impClass;
    }

    public static void saveClassAsTiff( int classId, ResultExportSettings resultExportSettings )
    {

        String className = resultExportSettings.classNames.get( classId );

        for ( int t = resultExportSettings.timePointsFirstLast[ 0 ]; t <= resultExportSettings.timePointsFirstLast[ 1 ]; ++t )
        {

            ImagePlus impClass = getBinnedAndProximityFilteredClassImage( classId, resultExportSettings, t );

            String path;

            if ( resultExportSettings.resultImagePlus.getNFrames() > 1 )
            {
                path = resultExportSettings.directory + File.separator + resultExportSettings.exportNamesPrefix + className + "--T" + String.format( "%05d", t ) + ".tif";
            }
            else
            {
                path = resultExportSettings.directory + File.separator + resultExportSettings.exportNamesPrefix + className + ".tif";
            }

            IJ.saveAsTiff( impClass, path );

            logDone( resultExportSettings, className, t, "Done with export of " );
        }

    }

    private static ImagePlus createImagePlusForClass( int classId, ResultExportSettings resultExportSettings )
    {

        String className = resultExportSettings.classNames.get( classId );

        final ImageStack stackOfAllTimepoints =
                new ImageStack(
                        resultExportSettings.inputImagePlus.getWidth(),
                        resultExportSettings.inputImagePlus.getHeight() );

        int numSlices = resultExportSettings.inputImagePlus.getNSlices();

        int numTimepoints =
                resultExportSettings.timePointsFirstLast[ 1 ]
                        - resultExportSettings.timePointsFirstLast[ 0 ] + 1;

        for ( int t = resultExportSettings.timePointsFirstLast[ 0 ];
              t <= resultExportSettings.timePointsFirstLast[ 1 ]; ++t )
        {
            ImagePlus impClass =
                    getBinnedAndProximityFilteredClassImage( classId, resultExportSettings, t );

            final ImageStack stackOfThisTimepoint = impClass.getStack();

            for ( int slice = 0; slice < numSlices; ++slice )
                stackOfAllTimepoints.addSlice( stackOfThisTimepoint.getProcessor( slice + 1 ) );
        }

        setSliceLabels(
                resultExportSettings.inputImagePlus.getStack(),
                stackOfAllTimepoints,
                className );

        final ImagePlus imp =
                new ImagePlus(
                        resultExportSettings.exportNamesPrefix + className,
                        stackOfAllTimepoints );

        imp.setDimensions( 1, numSlices, numTimepoints );
        imp.setOpenAsHyperStack( true );

        logDone( resultExportSettings, className, numTimepoints - 1, "Created " );

        return imp;

    }



    private static ImagePlus getBinnedRawDataFrame( ResultExportSettings resultExportSettings, int c, int t )
    {
        Duplicator duplicator = new Duplicator();

        ImagePlus rawDataFrame = duplicator.run( resultExportSettings.inputImagePlus, c + 1, c + 1, 1, resultExportSettings.inputImagePlus.getNSlices(), t + 1, t + 1 );

        if ( resultExportSettings.binning[ 0 ] * resultExportSettings.binning[ 1 ] * resultExportSettings.binning[ 2 ] > 1 )
        {
            Binner binner = new Binner();
            rawDataFrame = binner.shrink( rawDataFrame, resultExportSettings.binning[ 0 ], resultExportSettings.binning[ 1 ], resultExportSettings.binning[ 2 ], Binner.AVERAGE );
        }

        return rawDataFrame;
    }

    public static ArrayList< ImagePlus > exportResults( ResultExportSettings resultExportSettings )
    {

        logger.info( "Exporting results, using modality: " + resultExportSettings.exportType );

        if ( ! resultExportSettings.exportType.equals( ResultExportSettings.SHOW_IN_IMAGEJ ) )
        {
            logger.info( "Exporting results to: " + resultExportSettings.directory );
        }

        configureTimePointsExport( resultExportSettings );

        createExportDirectory( resultExportSettings );

        configureClassExport( resultExportSettings );

        configureExportBinning( resultExportSettings );

        exportRawData( resultExportSettings );

        final ArrayList< ImagePlus > classImps = exportClasses( resultExportSettings );

        createImarisHeader( resultExportSettings );

        logger.info( "Export of results finished." );

        return classImps;

    }

    private static void setSliceLabels(
            ImageStack source,
            ImageStack target,
            String className )
    {

        if ( source.getSize() != target.getSize() )
        {
            logger.info( "Results slice naming not yet " +
                    "implemented for multi-channel images." );
            return;
        }

        final int numImagePlanes = source.getSize();

        for ( int planeId = 0; planeId < numImagePlanes; ++planeId )
        {
            String sliceLabel = source.getSliceLabel( planeId + 1 );

            if ( sliceLabel != null )
            {
                if ( sliceLabel.contains( "." ) )
                {
                    final String[] split = sliceLabel.split( "\\." );
                    sliceLabel = split[ 0 ];
                }

                target.setSliceLabel( sliceLabel + "-" + className, planeId + 1 );
            }
            else
            {
                target.setSliceLabel( className, planeId + 1 );
            }
        }
    }

    private static void createExportDirectory( ResultExportSettings resultExportSettings )
    {
        if ( ! resultExportSettings.exportType.equals( ResultExportSettings.SHOW_IN_IMAGEJ ) )
        {
            IOUtils.createDirectoryIfNotExists( resultExportSettings.directory );
        }
    }

    private static void configureClassExport( ResultExportSettings resultExportSettings )
    {
        if ( resultExportSettings.classesToBeExported == null )
        {
            resultExportSettings.classesToBeExported = selectAllClasses( resultExportSettings.classNames );
        }
    }

    private static void configureTimePointsExport( ResultExportSettings resultExportSettings )
    {
        if ( resultExportSettings.timePointsFirstLast == null )
        {
            resultExportSettings.timePointsFirstLast = new int[2];
            resultExportSettings.timePointsFirstLast[ 0 ] = 0;
            resultExportSettings.timePointsFirstLast[ 1 ] = resultExportSettings.resultImagePlus.getNFrames() - 1;
        }
    }

    private static void createImarisHeader( ResultExportSettings resultExportSettings )
    {
        if ( resultExportSettings.exportType.equals( ResultExportSettings.SEPARATE_IMARIS ) )
        {
            ImarisUtils.createImarisMetaFile( resultExportSettings.directory );
        }
    }

    private static void exportRawData( ResultExportSettings resultExportSettings )
    {
        if ( resultExportSettings.exportType.equals( ResultExportSettings.SEPARATE_IMARIS ) )
        {
            if ( resultExportSettings.saveRawData )
            {
                if ( resultExportSettings.exportType.equals( ResultExportSettings.SEPARATE_IMARIS ) )
                {
                    saveRawDataAsImaris( resultExportSettings );
                }
                else if ( resultExportSettings.exportType.equals( ResultExportSettings.TIFF_STACKS ) )
                {
                    // TODO
                }
                else if ( resultExportSettings.exportType.equals( ResultExportSettings.SHOW_IN_IMAGEJ ) )
                {
                    // TODO
                }
            }
        }
    }

    private static ArrayList< ImagePlus > exportClasses( ResultExportSettings resultExportSettings )
    {
        final ArrayList< ImagePlus > classImps = new ArrayList<>();

        if ( resultExportSettings.exportType.equals( ResultExportSettings.SEPARATE_MULTI_CLASS_TIFF_SLICES ) )
        {
            saveAsSeparateMultiClassTiffSlices( resultExportSettings );
        }
        else
        {
            prepareProximityFilter( resultExportSettings );

            for ( int classIndex = 0; classIndex < resultExportSettings.classesToBeExported.size(); ++classIndex )
            {
                if ( resultExportSettings.classesToBeExported.get( classIndex ) )
                {
                    if ( resultExportSettings.exportType.equals( ResultExportSettings.SEPARATE_IMARIS ) )
                    {
                        saveClassAsImaris( classIndex, resultExportSettings );
                    }
                    else if ( resultExportSettings.exportType.equals( ResultExportSettings.TIFF_STACKS ) )
                    {
                        saveClassAsTiff( classIndex, resultExportSettings );
                    }
                    else if ( resultExportSettings.exportType.equals( ResultExportSettings.SHOW_IN_IMAGEJ ) )
                    {
                        createImagePlusForClass( classIndex, resultExportSettings ).show();
                    }
                    else if ( resultExportSettings.exportType.equals( ResultExportSettings.GET_AS_IMAGEPLUS_ARRAYLIST ) )
                    {
                        classImps.add( createImagePlusForClass( classIndex, resultExportSettings ) );
                    }
                }
            }
        }

        return classImps;

    }

    private static void saveAsSeparateMultiClassTiffSlices( ResultExportSettings resultExportSettings )
    {
        FinalInterval interval = resultExportSettings.resultImage.getInterval();

        String directory = resultExportSettings.directory;

        for ( long t = interval.min( T ); t <= interval.max( T ); ++t )
        {
            for ( long z = interval.min( Z ); z <= interval.max( Z ); ++z )
            {
                int slice = (int) z + 1;
                int frame = (int) t + 1;
                ImageProcessor ip = resultExportSettings.resultImage.getSlice( slice, frame);
                String filename = "classified--C01--T" + String.format( "%05d", frame ) + "--Z" + String.format( "%05d", slice ) + ".tif";
                String path = directory + File.separator + filename;
                IJ.saveAsTiff( new ImagePlus( filename, ip ), path );
            }
        }

    }

    private static void prepareProximityFilter( ResultExportSettings resultExportSettings )
    {
        ProximityFilterSettings settings = resultExportSettings.proximityFilterSettings;

        if (  settings.doSpatialProximityFiltering )
        {
            logger.info( "Computing proximity mask..." );
            ImagePlus impReferenceClass = getBinnedClassImage( settings.referenceClassId, resultExportSettings, 0  );
            settings.dilatedBinaryReferenceMask = ProximityFilter3D.getDilatedBinaryUsingEDT( impReferenceClass, settings.distanceInPixelsAfterBinning  );
        }
    }


    public static void saveClassesAsFiles( ResultExportSettings resultExportSettings )
    {
        // if ( checkMaximalVolume( resultImagePlus, binning, logger ) ) return;

        configureClassExport( resultExportSettings );

        configureExportBinning( resultExportSettings );

        for ( int classIndex = 0; classIndex < resultExportSettings.classesToBeExported.size(); ++classIndex )
        {
            if ( resultExportSettings.classesToBeExported.get( classIndex ) )
            {
                if ( resultExportSettings.exportType.equals( ResultExportSettings.SEPARATE_IMARIS ) )
                {
                    saveClassAsImaris( classIndex, resultExportSettings );
                }
                else if ( resultExportSettings.exportType.equals( ResultExportSettings.TIFF_STACKS ) )
                {
                    saveClassAsTiff( classIndex, resultExportSettings );
                }
            }
        }
    }


    public static void showClassesAsImages( ResultExportSettings resultExportSettings )
    {

        configureClassExport( resultExportSettings );

        configureExportBinning( resultExportSettings );

        for ( int classIndex = 0; classIndex < resultExportSettings.classesToBeExported.size(); ++classIndex )
        {
            if ( resultExportSettings.classesToBeExported.get( classIndex ) )
            {
                createImagePlusForClass( classIndex, resultExportSettings );
            }
        }

    }

    private static void configureExportBinning( ResultExportSettings resultExportSettings )
    {
        if ( resultExportSettings.binning == null )
        {
            resultExportSettings.binning = new int[] { 1, 1, 1 };
        }
    }

    public static final ArrayList< Boolean > selectAllClasses( ArrayList<String> classNames )
    {
        ArrayList< Boolean > classesToBeSaved = new ArrayList<>();
        for ( String className : classNames ) classesToBeSaved.add( true );
        return classesToBeSaved;
    }

    private static ImagePlus getClassImage( int classId, int t, ResultExportSettings settings)
    {
        Duplicator duplicator = new Duplicator();

        ImagePlus impClass = duplicator.run( settings.resultImagePlus,
                1, 1,
                1, settings.resultImagePlus.getNSlices(),
                t + 1, t + 1 );

        applyClassIntensityGate( classId, settings, impClass );

        convertToProperBitDepth( impClass, settings );

        return ( impClass );

    }

    private static void applyClassIntensityGate(
            int classId,
            ResultExportSettings settings,
            ImagePlus impClass )
    {
        int[] intensityGate = new int[]{
                classId * settings.classLutWidth + 1,
                (classId + 1 ) * settings.classLutWidth };

        applyIntensityGate( impClass, intensityGate );
    }

    public static void convertToProperBitDepth( ImagePlus impClass,
                                                ResultExportSettings settings )
    {
        int factorToFillBitDepth = (int) ( 255.0  / settings.classLutWidth );

        if ( settings.inputImagePlus.getBitDepth() == 16 )
        {
            IJ.run( impClass, "16-bit", "" );
            factorToFillBitDepth = (int) ( 65535.0  / settings.classLutWidth );
        }

        if ( settings.inputImagePlus.getBitDepth() == 32 )
        {
            IJ.run( impClass, "32-bit", "" );
            factorToFillBitDepth = (int) ( 255.0  / settings.classLutWidth );
        }

        IJ.run( impClass, "Multiply...", "value=" +
                factorToFillBitDepth + " stack");
    }
}
