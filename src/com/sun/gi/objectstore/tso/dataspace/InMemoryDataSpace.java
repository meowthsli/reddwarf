/*
 * Copyright © 2006 Sun Microsystems, Inc., 4150 Network Circle, Santa
 * Clara, California 95054, U.S.A. All rights reserved.
 * 
 * Sun Microsystems, Inc. has intellectual property rights relating to
 * technology embodied in the product that is described in this
 * document. In particular, and without limitation, these intellectual
 * property rights may include one or more of the U.S. patents listed at
 * http://www.sun.com/patents and one or more additional patents or
 * pending patent applications in the U.S. and in other countries.
 * 
 * U.S. Government Rights - Commercial software. Government users are
 * subject to the Sun Microsystems, Inc. standard license agreement and
 * applicable provisions of the FAR and its supplements.
 * 
 * Use is subject to license terms.
 * 
 * This distribution may include materials developed by third parties.
 * 
 * Sun, Sun Microsystems, the Sun logo and Java are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other
 * countries.
 * 
 * This product is covered and controlled by U.S. Export Control laws
 * and may be subject to the export or import laws in other countries.
 * Nuclear, missile, chemical biological weapons or nuclear maritime end
 * uses or end users, whether direct or indirect, are strictly
 * prohibited. Export or reexport to countries subject to U.S. embargo
 * or to entities identified on U.S. export exclusion lists, including,
 * but not limited to, the denied persons and specially designated
 * nationals lists is strictly prohibited.
 * 
 * Copyright © 2006 Sun Microsystems, Inc., 4150 Network Circle, Santa
 * Clara, California 95054, Etats-Unis. Tous droits réservés.
 * 
 * Sun Microsystems, Inc. détient les droits de propriété intellectuels
 * relatifs à la technologie incorporée dans le produit qui est décrit
 * dans ce document. En particulier, et ce sans limitation, ces droits
 * de propriété intellectuelle peuvent inclure un ou plus des brevets
 * américains listés à l'adresse http://www.sun.com/patents et un ou les
 * brevets supplémentaires ou les applications de brevet en attente aux
 * Etats - Unis et dans les autres pays.
 * 
 * L'utilisation est soumise aux termes de la Licence.
 * 
 * Cette distribution peut comprendre des composants développés par des
 * tierces parties.
 * 
 * Sun, Sun Microsystems, le logo Sun et Java sont des marques de
 * fabrique ou des marques déposées de Sun Microsystems, Inc. aux
 * Etats-Unis et dans d'autres pays.
 * 
 * Ce produit est soumis à la législation américaine en matière de
 * contrôle des exportations et peut être soumis à la règlementation en
 * vigueur dans d'autres pays dans le domaine des exportations et
 * importations. Les utilisations, ou utilisateurs finaux, pour des
 * armes nucléaires,des missiles, des armes biologiques et chimiques ou
 * du nucléaire maritime, directement ou indirectement, sont strictement
 * interdites. Les exportations ou réexportations vers les pays sous
 * embargo américain, ou vers des entités figurant sur les listes
 * d'exclusion d'exportation américaines, y compris, mais de manière non
 * exhaustive, la liste de personnes qui font objet d'un ordre de ne pas
 * participer, d'une façon directe ou indirecte, aux exportations des
 * produits ou des services qui sont régis par la législation américaine
 * en matière de contrôle des exportations et la liste de ressortissants
 * spécifiquement désignés, sont rigoureusement interdites.
 */

package com.sun.gi.objectstore.tso.dataspace;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.sun.gi.objectstore.NonExistantObjectIDException;

/**
 * InMemoryDataSpace is simple, non-persistant data store.
 * 
 * @author Jeff Kesselman
 * @version 1.0
 */
public class InMemoryDataSpace implements DataSpace {
    long appID;
    volatile boolean closed = false;
    Map<Long, byte[]> dataSpace = new LinkedHashMap<Long, byte[]>();

    Map<String, Long> nameSpace = new LinkedHashMap<String, Long>();
    Map<Long, String> reverseNameSpace = new HashMap<Long, String>(); 
    Set<Long> lockSet = new HashSet<Long>();

    private static Logger log =
	Logger.getLogger("com.sun.gi.objectstore.tso");

    private Object idMutex = new Object();
    private Object cachedStateMutex = new Object();

    private int id = 1;
	
    public InMemoryDataSpace(long appID) {
        this.appID = appID;
    }

    // internal routines to the system, used by transactions
    /*
     */
    private long getNextID() {
        synchronized (idMutex) {
            return id++;
        }
    }

    /**
     * {@inheritDoc}
     */
    public byte[] getObjBytes(long objectID) {
        synchronized (cachedStateMutex) {
            return dataSpace.get(new Long(objectID));
        }
    }

    /**
     * {@inheritDoc}
     */
    public void lock(long objectID) throws NonExistantObjectIDException {

        synchronized (lockSet) {
            while (lockSet.contains(objectID)) {
                try {
                    lockSet.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
	    synchronized (cachedStateMutex) {
		if (!dataSpace.containsKey(objectID)) {
		    lockSet.notifyAll();
		    throw new NonExistantObjectIDException();
		} else {
		    lockSet.add(new Long(objectID));
		    lockSet.notifyAll();
		}
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void release(long objectID) throws NonExistantObjectIDException {
        synchronized (lockSet) {
            lockSet.remove(new Long(objectID));
            lockSet.notifyAll();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void release(Set<Long> objectIDs)
            throws NonExistantObjectIDException
    {
        NonExistantObjectIDException re = null;

	synchronized (lockSet) {
	    
	    /*
	     * Attempt all of the releases.  Then if any of the
	     * releases threw an exception, pick the last one and
	     * rethrow it.  This is less than perfect.  -DJE
	     */

	    for (long oid : objectIDs) {
		try {
		    release(oid);
		} catch (NonExistantObjectIDException e) {
		    re = e;
		}
	    }

	    // If any of the releases threw an exception, throw it
	    // here.

	    if (re != null) {
		throw re;
	    }
	}
    }

    /**
     * {@inheritDoc}
     */
    public void atomicUpdate(boolean clear,
    	    Map<Long, byte[]> updateMap, List<Long> deleted)
	    throws DataSpaceClosedException {

	if (closed) {
	    throw new DataSpaceClosedException();
	}

	synchronized (lockSet) {
	    synchronized (cachedStateMutex) {

		dataSpace.putAll(updateMap);
		for (Long oid : deleted) {
		    lockSet.remove(oid);

		    String name = reverseNameSpace.get(oid);
		    if (name != null) {
			nameSpace.remove(name);
			reverseNameSpace.remove(oid);
		    }

		    dataSpace.remove(oid);
		}
	    }
	    lockSet.notifyAll();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Long lookup(String name) {
        synchronized (cachedStateMutex) {
            return nameSpace.get(name);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getAppID() {
        return appID;
    }

    /**
     * NOT IMPLEMENTED.
     * 
     * {@inheritDoc}
     */
    public void clear() {
	synchronized (cachedStateMutex) {
	    dataSpace.clear();
	    nameSpace.clear();
	    reverseNameSpace.clear();
	}
    }

    /**
     * {@inheritDoc}
     */
    public long create(byte[] data, String name) {
        Long createId;

        synchronized (cachedStateMutex) {
            if (name!=null){
		if (nameSpace.containsKey(name)) {
		    // System.out.println("Name space already contains "+name);
		    return DataSpace.INVALID_ID;
		}
		createId = new Long(getNextID());
            	nameSpace.put(name, createId);
            	reverseNameSpace.put(createId, name);
            } else {
		createId = new Long(getNextID());
	    }

	    if (data == null) {
		log.warning("creating null object " + createId);
	    }

            dataSpace.put(createId, data);
        }

        return createId;
    }

    /**
     * {@inheritDoc}
     */
    public void close() {
	closed = true;
    }
}