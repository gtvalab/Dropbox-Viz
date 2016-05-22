package forcespire.models;

import forcespire.controllers.EntityExtractorWrapper;
import forcespire.controllers.Parser;
import java.util.*;
import org.json.*;

/**
 * The data model represents all the loaded data, all the documents and the
 * entities linking them.
 *
 * About the model:
 * - There are many documents and many entities.
 * - Each entity can be linked to multiple documents.
 * - Multiple entities can be linked to a document.
 *
 * @author Patrick Fiaux, Alex Endert
 */
public class DataModel {

    private static enum EventType {

        ADDED, MODIFIED, REMOVED
    };
    /**
     * Assumes there are this many entities per document and creates
     * the entities collection to be the size of document count times this ratio.
     */
    private final static int DEFAULT_ENT_TO_DOC_RATIO = 16 * 8;
    /**
     * By default assumes this many documents.
     */
    private static final int DEFAULT_DOCUMENT_COUNT = 1024 * 8;
    /**
     * Document buffer, allows for some room to add documents without growing
     * the document collection. By default this many:
     */
    private static final int DOCUMENT_COUNT_BUFFER = 64 * 8;
    private ArrayList<Document> docs;
    private HashMap<String, Entity> entities;
    private ArrayList<Search> searches;
    private ArrayList<DataListener> listeners;
    private double totalStrength; //total amount of "energy" in system.

    /**
     * Default Constructor
     */
    public DataModel() {
        setup(DEFAULT_DOCUMENT_COUNT);
    }

    /**
     * Constructor with document count.
     * This allows the data model to initialize arrays to a good size.
     * @param document_count
     */
    public DataModel(int document_count) {
        setup(document_count);
    }

    /**
     * Construct data from loaded file.
     * @param data JSON object containing nodes and edges arrays
     * @throws JSONException
     */
    public DataModel(JSONObject data) throws JSONException {
        /*
         * Set up collections
         */
        setup(DEFAULT_DOCUMENT_COUNT);
        JSONArray jsonDocs, jsonEnts, jsonSearches;
        jsonDocs = data.getJSONArray("Documents");
        jsonEnts = data.getJSONArray("Entities");
        jsonSearches = data.getJSONArray("Searches");

        /*
         * Load Documents
         */
        //System.out.println(jsonDocs.toString(4));
        for (int i = 0; i < jsonDocs.length(); i++) {
            JSONObject doc;
            doc = jsonDocs.getJSONObject(i);
            //System.out.println("Loading document: " + doc);
            docs.add(new Document(doc));
        }

        /*
         * Load Entities
         */
        for (int i = 0; i < jsonEnts.length(); i++) {
            JSONObject ent;
            ent = jsonEnts.getJSONObject(i);
            //System.out.println("Loading entity: " + ent);
            Entity e = new Entity(ent, this);
            entities.put(e.getName().toLowerCase(), e);
        }

        /*
         * Load Searches
         */
        //System.out.println(jsonDocs.toString(4));
        for (int i = 0; i < jsonSearches.length(); i++) {
            JSONObject search;
            search = jsonSearches.getJSONObject(i);
            //System.out.println("Loading document: " + doc);
            searches.add(new Search(search));
        }
    }

    /**
     * This helper just sets up the array lists/hashes
     * Giving it the correct document_count for the model will avoid having to grow the array later and make the code run faster.
     * @param document_count number of documents expected.
     */
    private void setup(int document_count) {
        docs = new ArrayList<Document>(document_count + DOCUMENT_COUNT_BUFFER);
        entities = new HashMap<String, Entity>(document_count * DEFAULT_ENT_TO_DOC_RATIO);
        searches = new ArrayList<Search>();
        listeners = new ArrayList<DataListener>();
        totalStrength = entities.size() * 1.0;    //this is a default value, will change when ents get weights
    }

    /**
     * Add a listener to this model.
     * @param listener to add to the model
     */
    public void addDataListener(DataListener listener) {
        listeners.add(listener);
    }

    /**
     * Removes one of the data listener
     * @param listener listener to remove
     * @return true if listener was already registered and was removed
     */
    public boolean removeDataListener(DataListener listener) {
        return listeners.remove(listener);
    }

    /**
     * Look up a document based on a document id.
     * @param docId Id of document to find
     * @return null if document not found, document with this id otherwise
     */
    public synchronized Document lookUpDocument(int docId) {
        for (int i = 0; i < docs.size(); i++) {
            Document d = docs.get(i);
            if (d.getId() == docId) {
                return d;
            }
        }
        return null;
    }

    /**
     * Look up a search based on a search id.
     * @param searchID Id of search to find
     * @return null if search not found, search with this id otherwise
     */
    public synchronized Search lookUpSearch(int searchId) {
        for (int i = 0; i < searches.size(); i++) {
            Search s = searches.get(i);
            if (s.getId() == searchId) {
                return s;
            }
        }
        return null;
    }

    /**
     * Look up an entity based on entity ID. Used on loading.
     * @param entId Id of entity to find
     * @return Entity with id entID or null if not found.
     */
    public synchronized Entity lookUpEntity(int entId) {
        for (Entity e : entities.values()) {
            if (e.getID() == entId) {
                return e;
            }
        }
        return null;
    }

    /**
     * Add a search to the current model.
     * If a search already exists, this only increases the strength of the entity.
     * @param query String being searched
     * @param ent Entity for this search
     * @return Search object created/added
     */
    public synchronized Search addSearch(String query, Entity ent, int resutls) {
        Search s = new Search(query, ent, resutls);
        searches.add(s);
        fireSearchChange(s, EventType.ADDED);
        //ent.addSearch(s);
        link(ent, s);
        fireEntitySearchAdded(ent, s);
        return s;
    }

    /**
     * Add a blank document to the current model.
     * @return Blank document that was added
     */
    public synchronized Document addDocument() {
        Document d = new Document();
        docs.add(d);
        fireDocumentChange(d, EventType.ADDED, DataListener.OTHER);
        return d;
    }

    /**
     * Add a document with a set content
     * @param content content to give to document
     * @return newly created document
     */
    public synchronized Document addDocument(String content) {
        Document d = new Document(content);
        docs.add(d);
        fireDocumentChange(d, EventType.ADDED, DataListener.OTHER);
        return d;
    }

    /**
     * Add a document, passing in content and a name
     * @param content String to give the content of the document
     * @param name String to give the name of the document
     * @return newly created document
     */
    public synchronized Document addDocument(String content, String name) {
        Document d = new Document(content, name);
        docs.add(d);
        fireDocumentChange(d, EventType.ADDED, DataListener.OTHER);
        return d;
    }

    /**
     * Adds a document highlight to a document
     * @param d Document to highlight
     * @param start character index of highlight start
     * @param end character index of highlight end
     */
    public synchronized void addDocumentHighlight(Document d, int start, int end) {
        d.addHighlight(start, end);
        fireDocumentChange(d, EventType.MODIFIED, DataListener.HIGHLIGHT);
    }

    /**
     * Tries to remove a document
     * @param d Document to remove
     * @return true if it was removed, false if it was not
     */
    public synchronized boolean removeDocument(Document d) {
        boolean success = docs.remove(d);
        Iterator<Entity> entIt = d.iterator();
        while (entIt.hasNext()) {
            Entity e = entIt.next();
            e.removeDocument(d);
        }
        fireDocumentChange(d, EventType.REMOVED, DataListener.OTHER);
        return success;
    }

    /**
     * Tries to remove a search
     * @param s Search to remove
     * @return true if it was removed, false if it was not
     */
    public synchronized boolean removeSearch(Search s) {


        //remove the search from the entity
        Entity ent = s.getEntity();
        ent.removeSearch(s);
        fireEntitySearchRemoved(ent, s);

        //remove the search from the list of searches
        boolean success = searches.remove(s);
        fireSearchChange(s, EventType.REMOVED);

        return success;
    }

    /**
     * Returns a document at the index position.
     * @precondition index > 0 and < getDocumentCount
     * @param index document array index
     * @return requested document.
     */
    public Document getDocument(int index) {
        return docs.get(index);
    }

    /**
     * This updates the name of a document and fires events.
     * @param d Document to update
     * @param name New document name
     */
    public void setDocumentName(Document d, String name) {
        d.setName(name);
        fireDocumentChange(d, EventType.MODIFIED, DataListener.OTHER);
    }

    /**
     * Return the number of documents in the model.
     * @return document count.
     */
    public int getDocumentCount() {
        return docs.size();
    }

    /**
     * Return the number of entities in the model.
     * @return entity count.
     */
    public int getEntityCount() {
        return entities.size();
    }

    /**
     * Get an iterator for all the documents.
     * @return Document iterator
     */
    public Iterator<Document> documentIterator() {
        return docs.iterator();
    }

    /**
     * Sets the note for the document. Parses the note for entities,
     * then weights those entities.
     * Creates new entities from the notes based on the CPA entity extractor.
     * @param doc Document to which the note is added
     * @param str String of the note
     */
    public void setNote(Document doc, String str) {
        String old_str = doc.getNotes();
        doc.setNotes(str);
        fireDocumentChange(doc, EventType.MODIFIED, DataListener.NOTE);

        Parser localParser = new Parser();
        ArrayList<String> terms = localParser.parseString(str);

//        //remove the entities that are already in the data model
//        ArrayList<String> newEnts = new ArrayList<String>(ents);
//        for (String s : ents) {
//            if (hasEntity(s)) {
//                newEnts.remove(s);
//            }
//        }

        //if there are any ents remaining, they are new entities, add them to model
        for (String s : terms) {
            //addEntity(s, true);
            Entity e;
            if (hasEntity(s)) {
                //the entity is already in the model
                //TODO what should happen here?
                e = getEntity(s);
                fireEntityChange(e, EventType.MODIFIED, DataListener.ADDEDNOTE);
            } else {
                //the entity has to be added to the data model
                e = getEntity(s);
                fireEntityChange(e, EventType.ADDED, DataListener.ADDEDNOTE);
            }

            //check to see if doc contains the entity, if not, link it
            if (!doc.hasEntity(e)) {
                //entity is not in the document, link it
                this.link(e, doc);
            }
        }

        //parse the string for entities using parser, stopwords, etc.
        Parser parser = new Parser();
        //System.out.println("newTerms: " + str);
        ArrayList<String> newTerms = parser.parseString(str);
        //System.out.println("oldTerms: " + doc.getNotes());
        ArrayList<String> oldTerms = parser.parseString(old_str);

        //check what entities were in note before, but now removed
        for (String s : oldTerms) {
            if (!newTerms.contains(s)) {
                //this term has been removed from the note
                //TODO: check to see if it now occurs in no document, destroy entity?
                if (hasEntity(s)) {
                    //downweight the entity
                    Entity e = getEntity(s);
                    //e.setStrength(e.getStrength() / 1.5);

                    //remove entity from document
                    this.unlink(e, doc);
                    fireEntityChange(e, EventType.MODIFIED, DataListener.REMOVEDNOTE);
                }
            }
        }




        /*
         * Old way to parse the notes.
         * This used the cpa entity extractor.
         * The new way will treat all terms in the note as entities (excluding stopwords).
         *
        //parse the note for entities using the entity extractor
        ArrayList<String> ents = EntityExtractorWrapper.extractEntities(str);

        //remove the entities that are already in the data model
        ArrayList<String> newEnts = new ArrayList<String>(ents);
        for (String s : ents) {
        if (hasEntity(s)) {
        newEnts.remove(s);
        }
        }
        //if there are any ents remaining, they are new entities, add them to model
        for (String s : newEnts) {
        //addEntity(s, true);
        fireEntityChange(getEntity(s), EventType.ADDED, DataListener.ADDEDNOTE);
        }

        //parse the string for entities using parser, stopwords, etc.
        Parser parser = new Parser();
        //System.out.println("newTerms: " + str);
        ArrayList<String> newTerms = parser.parseString(str);
        //System.out.println("oldTerms: " + doc.getNotes());
        ArrayList<String> oldTerms = parser.parseString(old_str);

        //check how many new entities there are from last time
        for (String s : newTerms) {
        if (!oldTerms.contains(s)) {
        //this term is new and was not in the note before
        if (!hasEntity(s)) {
        //TODO: make new entity
        //Entity e = addEntity(s);
        //TODO: weight the new entity here
        } else {
        //upweight the existing entity
        Entity e = getEntity(s);

        //e.setStrength(e.getStrength() * 1.5);
        if (!doc.hasEntity(e)) {
        //entity is not in the document, link it
        this.link(e, doc);
        }
        fireEntityChange(e, EventType.MODIFIED, DataListener.ADDEDNOTE);
        }
        }
        }

        //TODO: how to allow an entity to be in a note multiple times?

        //check what entities were in note before, but now removed
        for (String s : oldTerms) {
        if (!newTerms.contains(s)) {
        //this term has been removed from the note
        //TODO: check to see if it now occurs in no document, destroy entity?
        if (hasEntity(s)) {
        //downweight the entity
        Entity e = getEntity(s);
        //e.setStrength(e.getStrength() / 1.5);

        //remove entity from document
        this.unlink(e, doc);
        fireEntityChange(e, EventType.MODIFIED, DataListener.REMOVEDNOTE);
        }
        }
        }
         *
         */

    }

    /**
     * Get an iterator for all the entities
     * @return Entity Iterator
     */
    public Iterator<Entity> entityIterator() {
        return entities.values().iterator();
    }

    /**
     * Get a search iterator for all the searches...
     * @return Search Iterator...
     */
    public Iterator<Search> searchIterator() {
        return searches.iterator();
    }

    /**
     * Checks to see if the entity already exists
     * @param name String name of the entity to check for existence
     * @return boolean true if exists, false if not
     */
    public boolean hasEntity(String name) {
        return entities.containsKey(name.toLowerCase());
    }

    /**
     * Adds a new entity from a string.
     * Will be called from the views (highlighting or adding entity)
     * @param name String the name of the entity that needs to get added
     * @param softdata true if it's a user created entity.
     * @return Entity the created entity
     */
    public Entity addEntity(String name, boolean softdata) {
        Entity e = new Entity(name, softdata);
        entities.put(name.toLowerCase(), e);
        fireEntityChange(e, EventType.ADDED, DataListener.OTHER);
        Parser parser = new Parser();

        //find the entity in the documents
        for (Document doc : docs) {
            ArrayList<String> docStrings = parser.parseString(doc.getContent());

            //check if the entity is in the document as a separate word (TRUCK)
            if (docStrings.contains(name)) {
                this.link(e, doc);
            } else if (name.length() > 2 && doc.getContent().toUpperCase().indexOf(name.toUpperCase()) != -1) {
                this.link(e, doc);
            }
        }
        //TODO should calculate the default weight of the entity somewhere

        //update the totalWeight in the system
        //updateTotalStrength(); this gets called in the event handler
        return e;
    }

    /**
     * Get an entity from the Entity list in the model (ignoring case!).
     * If the entity does not exist, create a new one, add to list, and return
     * @param name The name of the entity to look for.
     * @return Entity The entity in the Entity list in the model.
     */
    public Entity getEntity(String name) {
        String key = name.toLowerCase();
        Entity e = entities.get(key);

        if (e == null) {
            e = new Entity(name);
            entities.put(key, e);
            //System.out.println("Made new entity: " + e.getName());
            fireEntityChange(e, EventType.ADDED, DataListener.OTHER);
        }
        return e;
    }

    /**
     * Sets the name of the entity.
     * @param ent Entity that is getting a new name
     * @param name String the new name
     */
    public void setEntityName(Entity ent, String name) {
        ent.setName(name);
        fireEntityChange(ent, EventType.MODIFIED, DataListener.OTHER);
    }

    /**
     * Sets the strength of the entity.
     * @param ent Entity that is getting a new strength
     * @param strength Double the new strength
     * @param type int the type of change that has happened (DataListener.type)
     */
    public void setEntityStrength(Entity ent, double strength, int type) {

        ent.setStrength(strength);
        fireEntityChange(ent, EventType.MODIFIED, type);
    }

    /**
     * Removes the entity from the documents.
     * @param ent Entity being removed
     */
    public void removeEntity(Entity ent) {

        //remove the entity from each document's entity list
        Iterator<Document> docIt = ent.iterator();
        Document currentDoc;
        while (docIt.hasNext()) {
            currentDoc = docIt.next();
            currentDoc.removeEntity(ent);
            fireEntityDocumentRemoved(ent, currentDoc);
        }

        entities.remove(ent.getName().toLowerCase());

        fireEntityChange(ent, EventType.REMOVED, DataListener.OTHER);
    }

    /**
     * Updates the total strength of the data (sum of all entity strengths).
     * Should be called every time an entity is added
     * @return double the new total strength
     */
    public double updateTotalStrength() {
        double newTotal = 0.0;
        Iterator<Entity> ents = entityIterator();

        while (ents.hasNext()) {
            Entity ent = ents.next();
            newTotal += ent.getStrength();
        }

        totalStrength = newTotal; //set the totalweight, also return it

        //System.out.println("new total strength: " + totalStrength);

        return newTotal;
    }

    /**
     * This returns the current totalStength in the model.
     * @return double totalStrength
     */
    public double getTotalStrength() {
        return totalStrength;
    }

    /**
     * Updates all the strengths of the entities when an increase has occurred.
     * This ensures the same total strength is maintained in the system.
     * @param modifiedEnt The entity that has been modified
     * @param strengthChange How much total strength was added to the entity modified
     * @param type The interaction that has caused this change
     */
    public void updateAllStrength(Entity modifiedEnt, double strengthChange, int type) {
        double changePerEntity;
        double leftOver;
        double newEntStrength = 0;
        Iterator<Entity> ents = entityIterator();
        boolean anyEntsLeft = true;

        if (entities.size() > 1) {
            changePerEntity = strengthChange / (entities.size() - 1);
        } else {
            //there is only 1 other entity, must take all change from its strength
            changePerEntity = strengthChange;
        }

        leftOver = strengthChange;

        while (leftOver > 0 && anyEntsLeft) {
            ents = entityIterator();
            while (ents.hasNext()) {
                Entity ent = ents.next();
                anyEntsLeft = false;
                newEntStrength = ent.getStrength() - changePerEntity;
                //System.out.println("removing " + changePerEntity + " from " + ent.toString());
                if (!ent.equals(modifiedEnt)) {
                    if (newEntStrength > 0) {
                        ent.setStrength(newEntStrength);
                        leftOver -= changePerEntity;
                        anyEntsLeft = true;
                    } else {
                        ent.setStrength(0);
                        leftOver += Math.abs(newEntStrength);
                    }
                    fireEntityChange(ent, EventType.MODIFIED, type);
                }
            }
        }
    }

    /**
     * Adds the following:
     * -Entity gets added to the entity list in Document
     * -Document gets added to the document list in Entity
     * @param ent The entity to link
     * @param doc The document to link
     */
    public void link(Entity ent, Document doc) {
        //everytime a document is added to an entity, fire an event?
        if (!ent.hasDocument(doc)) {
            ent.addDocument(doc);   //doc gets added to doc list in entity
            fireEntityDocumentAdded(ent, doc);
        } else {
            //entity is already in the document, upweight the entity?? TODO
            //multiple occurances of entity in the document
            //should this only weight the entity more within this doc? global?
        }
        if (!doc.hasEntity(ent)) {
            doc.addEntity(ent);     //entity gets added to entity list in doc
        }
    }

    /**
     * Removes the following:
     * -Entity gets removed from the entity list in the Document
     * -Document gets removed from the document list in Document
     * @param ent Entity gets the Document removed
     * @param doc  Document gets the entity removed
     */
    public void unlink(Entity ent, Document doc) {
        if (ent.hasDocument(doc)) {
            ent.removeDocument(doc);
        }
        if (doc.hasEntity(ent)) {
            doc.removeEntity(ent);
        }
        fireEntityDocumentRemoved(ent, doc);
    }

    /**
     * Helper that fires a document event based on type
     * @param doc Document on which the event occurred
     * @param t type of the event
     * @param type the type of the event if it has modified the document
     */
    private void fireDocumentChange(Document doc, EventType t, int type) {
        for (DataListener d : listeners) {
            switch (t) {
                case ADDED:
                    d.documentAdded(doc);
                    break;
                case REMOVED:
                    d.documentRemoved(doc);
                    break;
                default:
                    d.documentModified(doc, type);
                    break;
            }
        }
    }

    /**
     * Helper that fires an entity event when a document is added to entity
     * @param e Entity  the document was added to
     * @param d Document that was added to the entity.
     */
    private void fireEntityDocumentAdded(Entity e, Document doc) {
        for (DataListener d : listeners) {
            d.entityDocumentAdded(e, doc);
        }
    }

    /**
     * Helper that fires an entity event when a entity is removed from a document
     * @param e Entity being removed
     * @param d Document from which it is removed
     */
    private void fireEntityDocumentRemoved(Entity e, Document doc) {
        for (DataListener d : listeners) {
            d.entityDocumentRemoved(e, doc);
        }
    }

    /**
     * Adds a search to an entity
     * @param ent Entity modifed
     * @param s Search modified
     */
    public void link(Entity ent, Search s) {
        //everytime a document is added to an entity, fire an event?
        if (!ent.hasSearch(s)) {
            ent.addSearch(s);   //doc gets added to doc list in entity
            fireEntitySearchAdded(ent, s);
        } else {
            //entity is already added to the search, do nothing?
        }
    }

    /**
     * Removes an search from an entity
     * @param ent Entity modified
     * @param s Search modified
     */
    public void unlink(Entity ent, Search s) {
        if (ent.hasSearch(s)) {
            ent.removeSearch(s);
        }
        //TODO: delete the search here?
        fireEntitySearchRemoved(ent, s);
    }

    private void fireEntitySearchAdded(Entity e, Search s) {
        for (DataListener d : listeners) {
            d.entitySearchAdded(e, s);
        }
    }

    private void fireEntitySearchRemoved(Entity e, Search s) {
        for (DataListener d : listeners) {
            d.entitySearchRemoved(e, s);
        }
    }

    /**
     * Helper that fires an entity event based on type
     * @param e Entity for which the event occurred.
     * @param t Type of event that occurred.
     */
    private void fireEntityChange(Entity e, EventType t, int type) {
        for (DataListener d : listeners) {
            switch (t) {
                case ADDED:
                    d.entityAdded(e);
                    updateTotalStrength();
                    break;
                case REMOVED:
                    d.entityRemoved(e);
                    break;
                default:
                    d.entityModified(e, type);
                    break;
            }
        }
    }

    /**
     * Helper that fired a search event based on the type
     * @param s Search for the event occurred
     * @param t Type of even that happened
     */
    private void fireSearchChange(Search s, EventType t) {
        for (DataListener d : listeners) {
            switch (t) {
                case ADDED:
                    d.searchAdded(s);
                    break;
                case REMOVED:
                    d.searchRemoved(s);
                    break;
                default:
                    //TODO: should there be a default?
                    break;
            }
        }
    }
}
