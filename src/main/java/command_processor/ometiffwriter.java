package command_processor;
import java.io.IOException;

import loci.common.DebugTools;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.FormatException;
import loci.formats.FormatTools;
import loci.formats.ImageWriter;
import loci.formats.meta.IMetadata;
import loci.formats.services.OMEXMLService;
import ome.units.UNITS;
import ome.units.quantity.Length;
import ome.units.unit.Unit;
import ome.xml.model.enums.DimensionOrder;
import ome.xml.model.enums.EnumerationException;
import ome.xml.model.enums.PixelType;
import ome.xml.model.primitives.PositiveInteger;
public class ometiffwriter {
    /** The file writer. */
    private ImageWriter writer;
    private Length pixelsizex;
    private Length pixelsizey;
    private Length pixelsizez;

    /** The name of the output file. */
    private String outputFile;
    public ometiffwriter(String outputFile) {
        this.outputFile = outputFile;
    }

    /** initialize the necessary variables */
    public void initialize(int xsize,int ysize,int zsize,float pixelsizex,float pixelsizey,float pixelsizez) {
        int pixelType = FormatTools.UINT16;
        DebugTools.setRootLevel("OFF");
        //set pixel size
        this.pixelsizex = new Length(pixelsizex,UNITS.MICROMETER);
        this.pixelsizey = new Length(pixelsizey,UNITS.MICROMETER);
        this.pixelsizez = new Length(pixelsizez,UNITS.MICROMETER);
        IMetadata omexml = initializeMetadata(xsize, ysize,zsize, pixelType);

        // only save a plane if the file writer was initialized successfully
        boolean initializationSuccess = initializeWriter(omexml);

    }

    /**
     * Set up the file writer.
     *
     * @param omexml the IMetadata object that is to be associated with the writer
     * @return true if the file writer was successfully initialized; false if an
     *   error occurred
     */
    private boolean initializeWriter(IMetadata omexml) {

        // create the file writer and associate the OME-XML metadata with it
        writer = new ImageWriter();
        System.out.println(writer.getCompression());
        writer.setMetadataRetrieve(omexml);

        Exception exception = null;
        try {
            writer.setId(outputFile);
        }
        catch (FormatException e) {
            exception = e;
        }
        catch (IOException e) {
            exception = e;
        }
        if (exception != null) {
            System.err.println("Failed to initialize file writer.");
            exception.printStackTrace();
        }
        return exception == null;
    }

    /**
     * Populate the minimum amount of metadata required to export an image.
     *
     * @param width the width (in pixels) of the image
     * @param height the height (in pixels) of the image
     * @param pixelType the pixel type of the image; @see loci.formats.FormatTools
     */
    private IMetadata initializeMetadata(int width, int height, int zdepth, int pixelType) {
        Exception exception = null;
        try {
            // create the OME-XML metadata storage object
            ServiceFactory factory = new ServiceFactory();
            OMEXMLService service = factory.getInstance(OMEXMLService.class);
            IMetadata meta = service.createOMEXMLMetadata();
            meta.createRoot();


            // define each stack of images - this defines a single stack of images
            meta.setImageID("Image:0", 0);
            meta.setPixelsID("Pixels:0", 0);

            // specify that the pixel data is stored in big-endian format
            // change 'TRUE' to 'FALSE' to specify little-endian format
//            for (int i=0;i<zdepth;i++){
                meta.setPixelsBinDataBigEndian(Boolean.FALSE, 0, 0);
//            }

            meta.setPixelsBigEndian(Boolean.FALSE,0);
            // specify that the images are stored in ZCT order
            meta.setPixelsDimensionOrder(DimensionOrder.XYZCT, 0);

            // specify that the pixel type of the images
            meta.setPixelsType(
                    PixelType.fromString(FormatTools.getPixelTypeString(pixelType)), 0);

            // specify the dimensions of the images
            meta.setPixelsSizeX(new PositiveInteger(width), 0);
            meta.setPixelsSizeY(new PositiveInteger(height), 0);
            meta.setPixelsSizeZ(new PositiveInteger(zdepth), 0);
            meta.setPixelsSizeC(new PositiveInteger(1), 0);
            meta.setPixelsSizeT(new PositiveInteger(1), 0);
            // specify the dimensions of the pixels
            meta.setPixelsPhysicalSizeX(this.pixelsizex,0);
            meta.setPixelsPhysicalSizeY(this.pixelsizey,0);
            meta.setPixelsPhysicalSizeZ(this.pixelsizez,0);
            // define each channel and specify the number of samples in the channel
            // the number of samples is 3 for RGB images and 1 otherwise
            meta.setChannelID("Channel:0:0", 0, 0);
            meta.setChannelSamplesPerPixel(new PositiveInteger(1), 0, 0);

            return meta;
        }
        catch (DependencyException e) {
            exception = e;
        }
        catch (ServiceException e) {
            exception = e;
        }
        catch (EnumerationException e) {
            exception = e;
        }

        System.err.println("Failed to populate OME-XML metadata object.");
        exception.printStackTrace();
        return null;
    }


    public void savePlane(int planeidx, byte[] data) {
        Exception exception = null;
        try {
            writer.saveBytes(planeidx, data);
        }
        catch (FormatException e) {
            exception = e;
        }
        catch (IOException e) {
            exception = e;
        }
        if (exception != null) {
            System.err.println("Failed to save plane.");
            exception.printStackTrace();
        }
    }


    /** Close the file writer. */
    public void cleanup() {
        try {
            writer.close();
        }
        catch (IOException e) {
            System.err.println("Failed to close file writer.");
            e.printStackTrace();
        }
    }

    /**
     * To export a file to OME-TIFF:
     *
     * $ java FileExport output-file.ome.tiff
     */
}
