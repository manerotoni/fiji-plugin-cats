package de.embl.cba.trainableDeepSegmentation.results;

import de.embl.cba.utils.logging.Logger;
import ij.ImagePlus;

import java.util.ArrayList;

public class ResultExportSettings
{
    public static final String SEPARATE_IMARIS = "Save as Imaris";
    public static final String SEPARATE_TIFF_FILES = "Save as Tiff stacks";
    public static final String SEPARATE_MULTI_CLASS_TIFF_SLICES = "Save as Tiff slices";
    public static final String SHOW_AS_SEPARATE_IMAGES = "Show images";
    public String directory;
    public String exportNamesPrefix = "";
    public ArrayList< Boolean > classesToBeExported;
    public int[] binning;
    public String exportType;
    public ProximityFilterSettings proximityFilterSettings = new ProximityFilterSettings();
    public ImagePlus rawData;
    public ImagePlus resultImagePlus;
    public ResultImage resultImage;
    public boolean saveRawData;
    public ArrayList< String > classNames;
    public Logger logger;
    public int classLutWidth;
    public int[] timePointsFirstLast;

}