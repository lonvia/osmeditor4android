package de.blau.android.views.layers;

import java.util.List;

import android.graphics.Path;

public abstract class StyleableLayer extends MapViewLayer {

    /**
     * Get the current color for this layer
     * 
     * @return the color as an int
     */
    abstract public int getColor();
    
    /**
     * Set the color for the layer
     * 
     * @param color color to use
     */
    abstract public void setColor(int color);

    /**
     * Get the stroke width for drawing lines
     * 
     * @return the stroke width in pixels
     */
    abstract public float getStrokeWidth();

    /**
     * Set the stroke width for lines
     * 
     * @param width stroke width in pixels
     */
    abstract public void setStrokeWidth(float width);
    
    /**
     * Symbol for points
     * 
     * @return the Path object used for points
     */
    abstract public Path getPointSymbol();
    
    /**
     * Set the Path for the symbol for points
     * 
     * @param symbol the Path for symbol
     */
    abstract public void setPointSymbol(Path symbol);
    
    /**
     * Set styling parameters back to defaults
     */
    abstract public void resetStyling();
    
    /**
     * Get a list of keys for labeling
     * 
     * @return a list og keys, null if there are none
     */
    public List<String> getLabelList() {
        return null;
    }
    
    /**
     * Set the key of the label to use
     * 
     * @param key label key to use
     */
    public void setLabel(String key) {
        // do nothing as default
    }
}
