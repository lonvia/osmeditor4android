package de.blau.android.osm;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.xmlpull.v1.XmlSerializer;

import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.Nullable;
import de.blau.android.App;
import de.blau.android.R;
import de.blau.android.presets.Preset;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.util.IssueAlert;

public abstract class OsmElement implements Serializable, XmlSerializable, JosmXmlSerializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7711945069147743670L;

	public static final long NEW_OSM_ID = -1;

	public static final byte STATE_UNCHANGED = 0;

	public static final byte STATE_CREATED = 1;

	public static final byte STATE_MODIFIED = 2;

	public static final byte STATE_DELETED = 3;

	long osmId;

	long osmVersion;

	SortedMap<String, String> tags;

	byte state;
	
	ArrayList<Relation> parentRelations;
	
	/**
	 * hasProblem() is an expensive test, so the results are cached.
	 * old version used a Boolean object which was silly we could naturally encode these as bits
	 */
	private boolean cachedHasProblem = false;
	private boolean checkedForProblem = false; // flag indicating if cachedHasProblem is valid
	
	OsmElement(final long osmId, final long osmVersion, final byte state) {
		this.osmId = osmId;
		this.osmVersion = osmVersion;
		this.tags = null;
		this.state = state;
		this.parentRelations = null;
	}

	public long getOsmId() {
		return osmId;
	}
	
	public long getOsmVersion() {
		return osmVersion;
	}
	
	void setOsmVersion(long osmVersion) {
		this.osmVersion = osmVersion;
	}

	void setOsmId(final long osmId) {
		this.osmId = osmId;
	}

	public SortedMap<String,String> getTags() {
		if (tags == null) {
			return Collections.unmodifiableSortedMap(new TreeMap<String, String>()); // for backwards compatibility
		}
		return Collections.unmodifiableSortedMap(tags);
	}

	/**
	 * @return true if the element has at least one tag
	 */
	public boolean hasTags() {
		return tags != null && tags.size() > 0; 
	}
	
	public byte getState() {
		return state;
	}

	/** gives a string description of the element type (e.g. 'node', 'way' or 'relation') - see also {@link #getType()} is rather confusingly named */
	abstract public String getName();

	/**
	 * Does not set the state if it's on CREATED, but if new state is DELETED.
	 * 
	 * @param newState
	 */
	void updateState(final byte newState) {
		if (state != STATE_CREATED || newState == STATE_DELETED) {
			state = newState;
		}
	}

	void setState(final byte newState) {
		state = newState;
	}

	void addOrUpdateTag(final String tag, final String value) {
		if (tags==null) {
			tags = new TreeMap<String, String>();
		}
		tags.put(tag, value);
		checkedForProblem = false;
	}

	/**
	 * Add the tags of the element, replacing any existing tags.
	 * @param tags New tags to add or to replace existing tags.
	 */
	void addTags(final Map<String, String> tags) {
		if (tags != null) {
			if (this.tags==null) {
				this.tags = new TreeMap<String, String>();
			}
			this.tags.putAll(tags);
			checkedForProblem = false;
		}
	}

	/**
	 * Set the tags of the element, replacing all existing tags.
	 * @param tags New tags to replace existing tags.
	 * @return Flag indicating if the tags have actually changed.
	 */
	boolean setTags(@Nullable final Map<String, String> tags) {
		if (this.tags == null) {
			addTags(tags);
			return true;
		} else if (!this.tags.equals(tags)) {
			this.tags.clear();
			addTags(tags);
			return true;
		}
		return false;
	}

	/**
	 * @param key the key to search for (case sensitive)
	 * @param value the value to search for (case sensitive)
	 * @return true if the element has a tag with this key and value.
	 */
	public boolean hasTag(final String key, final String value) {
		if (tags == null) {
			return false;
		}
		String keyValue = tags.get(key);
		return keyValue != null && keyValue.equals(value);
	}
	
	/**
	 * @param tags tags to use instead of the standard ones
	 * @param key the key to search for (case sensitive)
	 * @param value the value to search for (case sensitive)
	 * @return true if the element has a tag with this key and value.
	 */
	boolean hasTag(final Map<String, String> tags, final String key, final String value) {
		if (tags == null) {
			return false;
		}
		String keyValue = tags.get(key);
		return keyValue != null && keyValue.equals(value);
	}

	/**
	 * @param key the key to search for (case sensitive)
	 * @return the value of this key.
	 */
	public String getTagWithKey(final String key) {
		if (tags == null) {
			return null;
		}
		return tags.get(key);
	}

	/**
	 * @param key the key to search for (case sensitive)
	 * @return true if the element has a tag with this key.
	 */
	public boolean hasTagKey(final String key) {
		return getTagWithKey(key) != null;
	}
	
	/**
	 * check if this element has tags of any kind
	 * @return
	 */
	public boolean isTagged() {
		return (tags != null) && (tags.size() > 0);
	}
	
	/**
	 * Merge the tags from two OsmElements into one set.
	 * @param e1
	 * @param e2
	 * @return
	 */
	public static Map<String, String> mergedTags(OsmElement e1, OsmElement e2) {
		Map<String, String> merged = new TreeMap<String, String>(e1.getTags());
		Map<String, String> fromTags = e2.getTags();
		for (String key : fromTags.keySet()) {
			Set<String> values = new HashSet<String>(Arrays.asList(fromTags.get(key).split("\\;")));
			if (merged.containsKey(key)) {
				values.addAll(Arrays.asList(merged.get(key).split("\\;")));
			}
			StringBuilder b = new StringBuilder();
			for (String v : values) {
				if (b.length() > 0) b.append(';');
				b.append(v);
			}
			merged.put(key, b.toString());
		}
		return merged;
	}

	@Override
	public String toString() {
		return getName() + " " + osmId;
	}

	void tagsToXml(final XmlSerializer s) throws IllegalArgumentException,
			IllegalStateException, IOException {
		if (tags != null) {
			for (Entry<String, String> tag : tags.entrySet()) {
				s.startTag("", "tag");
				s.attribute("", "k", tag.getKey());
				s.attribute("", "v", tag.getValue());
				s.endTag("", "tag");
			}
		}
	}

	public boolean isUnchanged() {
		return state == STATE_UNCHANGED;
	}
	
	/**
	 * Add reference to parent relation 
	 * Does not check id to avoid dups!
	 */
	public void addParentRelation(Relation relation) {
		if (parentRelations == null) {
			parentRelations = new ArrayList<Relation>();
		}
		parentRelations.add(relation);
	}
	
	/**
	 * Check for parent relation
	 * @param relation
	 * @return
	 */
	public boolean hasParentRelation(Relation relation) {
		return (parentRelations != null && parentRelations.contains(relation));
	}
	
	/**
	 * Check for parent relation based on id
	 * @param relation
	 * @return
	 */
	public boolean hasParentRelation(long osmId) {
		if (parentRelations == null) {
			return false;
		}
		for (Relation r:parentRelations) {
			if (osmId == r.getOsmId())
				return true;
		}
		return false;
	}
	
	/**
	 * Add all parent relations, avoids dups
	 */
	public void addParentRelations(ArrayList<Relation> relations) {
		if (parentRelations == null) {
			parentRelations = new ArrayList<Relation>();
		}
		//  dedup
		for (Relation r : relations) {
			if (!parentRelations.contains(r)) {
				addParentRelation(r);
			}
		}
	}
	
	public ArrayList<Relation> getParentRelations() {
		return parentRelations;
	}
	
	public boolean hasParentRelations() {
		return (parentRelations != null) && (parentRelations.size() > 0);
	}
	
	/**
	 * Remove reference to parent relation
	 * does not check for id
	 */
	public void removeParentRelation(Relation relation) {
		if (parentRelations != null) {
			parentRelations.remove(relation);
		}
	}
	
	/**
	 * Remove reference to parent relation
	 */
	public void removeParentRelation(long osmId) {
		if (parentRelations != null) {
			ArrayList<Relation> tempRelList = new ArrayList<Relation>(parentRelations);
			for (Relation r:tempRelList) {
				if (osmId == r.getOsmId())
					parentRelations.remove(r);
			}
		}
	}


	/**
	 * Generate a human-readable description/summary of the element.
	 * @return A description of the element.
	 */
	public String getDescription() {
		return getDescription(true);
	}
	
	/**
	 * Generate a human-readable description/summary of the element.
	 * @return A description of the element.
	 */
	public String getDescription(Context ctx) {
		return getDescription(ctx, true);
	}
	
	/**
	 * Return a concise description of the element
	 * @param withType
	 * @return
	 */
	public String getDescription(boolean withType) {
		return getDescription(null, withType);
	}
	
	/**
	 * Return a concise description of the element
	 * @param withType
	 * @return
	 */
	private String getDescription(Context ctx, boolean withType) {
		// Use the name if it exists
		String name = getTagWithKey(Tags.KEY_NAME);
		if (name != null && name.length() > 0) {
			return name;
		}
		// Then the address
		String housenumber = getTagWithKey(Tags.KEY_ADDR_HOUSENUMBER);
		if (housenumber != null && housenumber.length() > 0) {
			try {
				String street = getTagWithKey(Tags.KEY_ADDR_STREET);
				if (street != null && street.length() > 0) {
					if (ctx != null) {
						return ctx.getResources().getString(R.string.address_housenumber_street, street, housenumber);
					} else {
						return "address " + housenumber + " " + street;
					}
				} else {
					if (ctx != null) {
						return ctx.getResources().getString(R.string.address_housenumber, housenumber);
					} else {
						return "address " + housenumber;
					}
				}
			} catch (Exception ex) {
				// protect against translation errors
			}
		}
		// try to match with a preset
		if (ctx != null) {
			PresetItem p = Preset.findBestMatch(App.getCurrentPresets(ctx),tags);
			if (p!=null) {
				String ref = getTagWithKey(Tags.KEY_REF);
				return p.getTranslatedName() + (ref != null ? " " + ref : "") ;
			}
		}
		// Then the value of the most 'important' tag the element has
		String tag = getPrimaryTag();
		if (tag != null) {
			return (withType ? getName() + " " : "") + tag;
		}
		
		// Failing the above, the OSM ID
		return (withType ? getName() + " #" : "#") + Long.toString(getOsmId());
	}
	
	/**
	 * @return the first kay =value of any important tags or null if none found
	 */
	public String getPrimaryTag() {
		for (String tag : Tags.importantTags) {
			String value = getTagWithKey(tag);
			if (value != null && value.length() > 0) {
				return  tag + "=" + value;
			}
		}
		return null;
	}
	
	/**
	 * Generate a description of the element that also includes state information.
	 * @param aResources Application resources.
	 * @return A human readable description of the element that includes state information.
	 */
	public String getStateDescription(final Resources aResources) {
		int resid;
		switch (getState()) {
		case STATE_CREATED:
			resid = R.string.changes_created;
			break;
		case STATE_MODIFIED:
			resid = R.string.changes_changed;
			break;
		case STATE_DELETED:
			resid = R.string.changes_deleted;
			break;
		default:
			resid = 0;
			break;
		}
		String result = getDescription();
		if (resid != 0) {
			result = aResources.getString(resid, result);
		}
		return result;
	}
	
	/**
	 * Test if the element has any problems by searching all the tags for the words
	 * "fixme" or "todo".
	 * @return true if the element has any noted problems, false otherwise.
	 */
	boolean calcProblem() {
		final String pattern = "(?i).*\\b(?:fixme|todo)\\b.*";
		if (tags != null) {
			for (String key : tags.keySet()) {
				// test key and value against pattern
				if (key.matches(pattern) || tags.get(key).matches(pattern)) {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * return a string giving the problem
	 */
	public String describeProblem() {
		final String pattern = "(?i).*\\b(?:fixme|todo)\\b.*";
		if (tags != null) {
			for (String key : tags.keySet()) {
				// test key and value against pattern
				if (key.matches(pattern) || tags.get(key).matches(pattern)) {
					return key + ": " + tags.get(key);
				}
			}
		}
		return "";
	}
	
	/**
	 * Test if the element has a noted problem. A noted problem is where someone has
	 * tagged the element with a "fixme" or "todo" key/value.
	 * @return true if the element has a noted problem, false if it doesn't.
	 */
	public boolean hasProblem(Context context) {
		// This implementation assumes that calcProblem() may be expensive, and
		// caches the calculation.
		if (!checkedForProblem) {
			checkedForProblem = true; // don't re-check
			cachedHasProblem = calcProblem();
			if (cachedHasProblem && context != null) {
				IssueAlert.alert(context, this); 
			}
		}
		return cachedHasProblem;
	}
	
	/**
	 * Call if you have made a change that potentially changes the problem state of the element
	 */
	public void resetHasProblem() {
		checkedForProblem = false;
	}
	
	/** (see also {@link #getName()} - this returns the full type, differentiating between open and closed ways) 
	 * @return the {@link ElementType} of the element */
	public abstract ElementType getType();
	
	/**
	 * Version of above that uses a potential different set of tags
	 * @param tags
	 * @return
	 */
	public abstract ElementType getType(Map<String, String>tags);
	
	/** Enum for element types (Node, Way, Closed Ways, Relations, Areas (MPs) */
	public enum ElementType {
		NODE,
		WAY,
		CLOSEDWAY,
		RELATION,
		AREA
	}

	/**
	 * Return a bounding box covering the element
	 * @return
	 */
	public abstract BoundingBox getBounds();
}
