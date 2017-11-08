package trainableDeepSegmentation.training;

import bigDataTools.logging.IJLazySwingLogger;
import bigDataTools.logging.Logger;
import weka.core.Instances;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class InstancesManager {

    Logger logger = new IJLazySwingLogger();

    Map< String, Instances > instancesMap = null;

    public InstancesManager()
    {
        instancesMap = new HashMap<>();
    }

    public void putInstances( Instances instances )
    {
        instancesMap.put( instances.relationName(), instances );
    }

    public Instances getInstances( String key )
    {
        return ( instancesMap.get( key ) );
    }

    public Set< String > getKeys()
    {
        return ( instancesMap.keySet() );
    }

    /**
     * Write current instancesMap into an ARFF file
     * @param instances set of instancesMap
     * @param filename ARFF file name
     */
    private boolean saveInstancesToARFF( Instances instances,
                                        String directory,
                                        String filename)
    {

        BufferedWriter out = null;
        try{
            out = new BufferedWriter(
                    new OutputStreamWriter(
                            new FileOutputStream( directory
                                    + File.separator + filename ) ) );

            final Instances header = new Instances(instances, 0);
            out.write(header.toString());

            for(int i = 0; i < instances.numInstances(); i++)
            {
                out.write(instances.get(i).toString()+"\n");
            }
        }
        catch(Exception e)
        {
            logger.error("Error: couldn't write instancesMap into .ARFF file.");
            e.printStackTrace();
            return false;
        }
        finally{
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        return true;
    }


    public boolean saveInstancesToARFF( String key,
                                         String directory,
                                         String filename)
    {
        boolean status = saveInstancesToARFF( instancesMap.get( key ),
                directory, filename );
        return status;
    }


    private void extractFeatureSettingsFromInstances()
    {
        // maybe simply put the settings into each feature name....

        // Check the features that were used in the loaded data
        /*
        Enumeration<Attribute> attributes = loadedTrainingData.enumerateAttributes();
        final int numFeatures = FeatureStack.availableFeatures.length;
        boolean[] usedFeatures = new boolean[numFeatures];
        while(attributes.hasMoreElements())
        {
            final Attribute a = attributes.nextElement();
            for(int i = 0 ; i < numFeatures; i++)
                if(a.name().startsWith(FeatureStack.availableFeatures[i]))
                    usedFeatures[i] = true;
        }*/
    }


    public String putInstancesFromARFF( String directory, String filename )
    {
        Instances instances = loadInstancesFromARFF( directory, filename );
        if ( instances == null ) return null;



        String key = filename.substring(0, filename.lastIndexOf('.'));
        instancesMap.put( key, instances );

        return key;
    }

    /**
     * Read ARFF file
     * @param filename ARFF file name
     * @return set of instancesMap read from the file
     */
    private Instances loadInstancesFromARFF( String directory, String filename )
    {
        String pathname = directory + File.separator + filename;

        logger.info("Loading data from " + pathname + "...");

        try{
            BufferedReader reader = new BufferedReader(
                    new FileReader( pathname ));
            try{
                Instances data = new Instances( reader );
                // setting class attribute
                data.setClassIndex(data.numAttributes() - 1);
                reader.close();
                return data;
            }
            catch(IOException e)
            {
                logger.error("IOException");
            }
        }
        catch(FileNotFoundException e)
        {
            logger.error("File not found!");
        }

        return null;
    }


}
