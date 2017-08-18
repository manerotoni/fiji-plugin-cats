
Trainable Deep Weka Segmentation
======================
The **Trainable Deep Weka Segmentation** is a Fiji plugin for trainable segmentation using deep convolution. It is heavily based on the original **Trainable Weka Segmentation** plugin.

## Software requirements

You need Fiji running Java 1.8; older version will run Java 1.6, which does not work for this plugin. The easiest way to have Fiji running Java 1.8 is to reinstall Fiji from scratch.

## Hardware requirements

The plugin should work on any computer however the computations are quite heavy and can thus be slow. The code is multi-threaded such that the execution speed scales basically linearly with the number of cores. 

Examples:
- For a convolution depth of 3 we observed a pixel classification speed of ~100 kiloVoxel / second using a 32 core Linux CentOS 7 machine.
- For a convolution depth of 3 we observed a pixel classification speed of ~10 kiloVoxel / second using a 4 core MacBook Air.

## Installation

Download below files and place them in your Fiji plugins folder:
- https://git.embl.de/grp-almf/fiji-plugin-deep-segmentation/raw/master/out/artifacts/fiji_plugin_trainable_deep_segmentation.jar
- https://github.com/tischi/fiji-plugin-bigDataTools/raw/master/out/artifacts/fiji--bigDataTools_.jar

The latter plugin enables streaming large data sets from disk.

## Usage

### Open a data set

You can either simply load a data set into Fiji or you can use [Plugins > BigDataTools > DataStreamingTools] in order to stream a data set from disk; this is useful for data sets that come close to or exceed the RAM of your computer. 

Once you opened the data set you launch the segmentation via [Plugins > Segmentation > Trainable Deep Segmentation]; the graphical user interface will appear around your data set.

Supported data types:

- The streaming currenlty only works for Tiff or Hdf5 based data.
- The trainable segmentation supports:
    - 2D+c+t, 3D+c+t
    - spatially anisotropic data 

### Settings

...

### Tips and tricks

#### How to put your training labels 

As this tool is able to learn long range context you have to really tell it what you want. I recommend always putting a background label just next to the actual label.

