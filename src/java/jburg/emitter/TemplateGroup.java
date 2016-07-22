package jburg.emitter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jburg.version.JBurgVersion;

import org.stringtemplate.v4.*;

/**
 * TemplateGroup is a facade for a STGroup that
 * hides details of the .stg file management, supports name/value
 * attribute pairs for convenience, and manages a map of default
 * attributes to be supplied to all templates.
 */
public class TemplateGroup
{
    public TemplateGroup(String directoryName, String templateGroupName)
    {
        if (directoryName != null) {
            templateGroupName = directoryName + "/" + templateGroupName;
        }
        this.templates = new STGroupFile(templateGroupName);

        setDefaultAttribute("timestamp", new java.util.Date());
        setDefaultAttribute("version", JBurgVersion.version);
    }

    /**
     * The backing STGroup.
     */
    private STGroup templates;

    /**
     * Default attributes to provide to all templates.
     * Specific values are up to the caller.
     */
    private Map<String, Object> defaultAttrs = new HashMap<String, Object>();

    private Object configuration;

    void setConfiguration(Object configuration)
    {
        this.configuration = configuration;
    }

    /**
     * Select a template by name, with optional attributes.
     * The template is also given a copy of the template
     * group's current set of default attributes.
     * @param name the template's name.
     * @param attrValue pairs of attrName, attrValue.
     */
    public ST getTemplate(String name, Object ... attrValue)
    {
        Map<String,Object> templateAttrs = new HashMap<String, Object>();
        templateAttrs.put("defaults", this.defaultAttrs);
        templateAttrs.put("config", this.configuration);
        ST result = this.templates.getInstanceOf(name);

        // Emulate ST3 default attributes behavior.
        for (String key: defaultAttrs.keySet()) {
            try {
                result.add(key, defaultAttrs.get(key));
            } catch (java.lang.IllegalArgumentException noSuchAttr) {
                // Continue.
            }
        }

        if (! (attrValue.length == 0 || attrValue.length % 2 == 0) )
            throw new IllegalStateException("Expected an even number of attr/value pairs");

        for ( int i = 0; i < attrValue.length; i += 2 )
        {
            result.add(attrValue[i].toString(), attrValue[i+1]);
        }

        return result;
    }

    /**
     * Register a renderer; delegates to the STGroup's registerRenderer method.
     */
    public void registerRenderer(Class<? extends Object> clazz, AttributeRenderer renderer)
    {
        this.templates.registerRenderer(clazz, renderer);
    }

    /**
     * Set a default attriburte.
     * @param key the attribute's name.
     * @param value the attribute's value.
     */
    public void setDefaultAttribute(String key, Object value)
    {
        this.defaultAttrs.put(key,value);
    }
}

