package it.unibz.krdb.obda.owlrefplatform.core.abox;

import it.unibz.krdb.obda.model.Atom;
import it.unibz.krdb.obda.model.CQIE;
import it.unibz.krdb.obda.model.OBDADataSource;
import it.unibz.krdb.obda.model.DatalogProgram;
import it.unibz.krdb.obda.model.OBDADataFactory;
import it.unibz.krdb.obda.model.OBDAMappingAxiom;
import it.unibz.krdb.obda.model.OBDAModel;
import it.unibz.krdb.obda.model.Predicate;
import it.unibz.krdb.obda.model.Atom;
import it.unibz.krdb.obda.model.URIConstant;
import it.unibz.krdb.obda.model.ValueConstant;
import it.unibz.krdb.obda.model.impl.OBDADataFactoryImpl;
import it.unibz.krdb.obda.model.impl.RDBMSourceParameterConstants;
import it.unibz.krdb.obda.owlrefplatform.core.ontology.ABoxAssertion;
import it.unibz.krdb.obda.owlrefplatform.core.ontology.imp.DLLiterAttributeABoxAssertionImpl;
import it.unibz.krdb.obda.owlrefplatform.core.ontology.imp.DLLiterConceptABoxAssertionImpl;
import it.unibz.krdb.obda.owlrefplatform.core.ontology.imp.DLLiterRoleABoxAssertionImpl;
import it.unibz.krdb.obda.owlrefplatform.core.queryevaluation.JDBCUtility;
import it.unibz.krdb.obda.owlrefplatform.core.srcquerygeneration.ComplexMappingSQLGenerator;
import it.unibz.krdb.obda.owlrefplatform.core.unfolding.ComplexMappingUnfolder;
import it.unibz.krdb.obda.owlrefplatform.core.viewmanager.MappingViewManager;
import it.unibz.krdb.sql.JDBCConnectionManager;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/***
 * Allows you to work with the virtual triples defined by an OBDA model. In
 * particular you will be able to generate ABox assertions from it and get
 * statistics.
 * 
 * The class works "online", that is, the triples are never kept in memory by
 * this class, instead a connection to the data sources will be established when
 * needed and the data will be streamed or retrieved on request.
 * 
 * In order to compute the SQL queries relevant for each predicate we use the
 * ComplexMapping Query unfolder. This allow us to reuse all the infrastructure
 * for URI generation and to avoid manually processing each mapping. This also
 * means that all features and limitations of the complex unfolder are present
 * here.
 * 
 * @author Mariano Rodriguez Muro
 * 
 */
public class VirtualABoxMaterializer {

	OBDAModel									model;

	OBDADataFactory								obdafac				= OBDADataFactoryImpl.getInstance();

	JDBCConnectionManager						jdbcMan				= JDBCConnectionManager.getJDBCConnectionManager();

	Map<OBDADataSource, ComplexMappingUnfolder>		unfoldersMap		= new HashMap<OBDADataSource, ComplexMappingUnfolder>();

	Map<OBDADataSource, ComplexMappingSQLGenerator>	sqlgeneratorsMap	= new HashMap<OBDADataSource, ComplexMappingSQLGenerator>();

	Set<Predicate>								vocabulary			= new LinkedHashSet<Predicate>();

	/***
	 * Collects the vocabulary of this OBDA model and initializes unfodlers and
	 * SQL generators for the model.
	 * 
	 * @param model
	 * @throws Exception
	 */
	public VirtualABoxMaterializer(OBDAModel model) throws Exception {
		this.model = model;

		for (OBDADataSource source : model.getSources()) {
			List<OBDAMappingAxiom> maps = model.getMappings(source.getSourceID());

			/*
			 * Preparing unfolders and sql generators for each source so that
			 * this is only is done once
			 */

			MappingViewManager vewman = new MappingViewManager(maps);
			ComplexMappingUnfolder unfolder = new ComplexMappingUnfolder(maps, vewman);
			JDBCUtility util = new JDBCUtility(source.getParameter(RDBMSourceParameterConstants.DATABASE_DRIVER));
			ComplexMappingSQLGenerator sqlgen = new ComplexMappingSQLGenerator(vewman, util);

			unfoldersMap.put(source, unfolder);
			sqlgeneratorsMap.put(source, sqlgen);

			/*
			 * Collecting the vocabulary of the mappings
			 */

			for (OBDAMappingAxiom map : maps) {
				CQIE targetq = (CQIE) map.getTargetQuery();
				for (Atom atom : targetq.getBody()) {
					if (!vocabulary.contains(atom.getPredicate())) {
						vocabulary.add(atom.getPredicate());
					}
				}
			}
		}

	}

	public Iterator<ABoxAssertion> getAssertionIterator() throws Exception {
		return new VirtualTriplePredicateIterator(vocabulary.iterator(), model.getSources(), unfoldersMap, sqlgeneratorsMap);
	}

	public List<ABoxAssertion> getAssertionList() throws Exception {
		Iterator<ABoxAssertion> it = getAssertionIterator();
		List<ABoxAssertion> assertions = new LinkedList<ABoxAssertion>();
		while (it.hasNext()) {
			assertions.add(it.next());
		}
		return assertions;
	}

	public Iterator<ABoxAssertion> getAssertionIterator(Predicate p) throws Exception {
		return new VirtualTripleIterator(p, this.model.getSources(), this.unfoldersMap, this.sqlgeneratorsMap);
	}

	/***
	 * Returns the list of all triples (ABox assertions) generated by the
	 * mappings of the OBDA model. Note that this list is not linked to the DB,
	 * that is, changes to the list do not affect the database.
	 * 
	 * This is only a convenience method since, internally, this method simply
	 * calls the getAssertionIterator method.
	 * 
	 * @param p
	 * @return
	 * @throws Exception
	 */
	public List<ABoxAssertion> getAssertionList(Predicate p) throws Exception {
		VirtualTripleIterator it = new VirtualTripleIterator(p, this.model.getSources(), this.unfoldersMap, this.sqlgeneratorsMap);

		List<ABoxAssertion> assertions = new LinkedList<ABoxAssertion>();
		while (it.hasNext()) {
			assertions.add(it.next());
		}
		return assertions;

	}

	public int getTripleCount() throws Exception {
		Iterator<ABoxAssertion> it = getAssertionIterator();

		int tripleCount = 0;
		while (it.hasNext()) {
			it.next();
			tripleCount += 1;
		}

		return tripleCount;

	}

	/***
	 * Counts the number of triples generated by the mappings for this
	 * predicate. Note, this method will actually execute queries and iterate
	 * over them to count the results, this might take a long time depending on
	 * the size of the DB.
	 * 
	 * @param pred
	 * @return
	 * @throws Exception
	 */
	public int getTripleCount(Predicate pred) throws Exception {

		Iterator<ABoxAssertion> it = getAssertionIterator(pred);

		int tripleCount = 0;
		while (it.hasNext()) {
			it.next();
			tripleCount += 1;
		}

		return tripleCount;
	}

	public class VirtualTriplePredicateIterator implements Iterator<ABoxAssertion> {

		private Iterator<Predicate>							predicates;
		private Collection<OBDADataSource>						sources;
		private Map<OBDADataSource, ComplexMappingUnfolder>		unfolders;
		private Map<OBDADataSource, ComplexMappingSQLGenerator>	sqlgens;

		private Predicate									currentPredicate	= null;
		private VirtualTripleIterator						currentIterator		= null;

		public VirtualTriplePredicateIterator(Iterator<Predicate> predicates, Collection<OBDADataSource> sources,
				Map<OBDADataSource, ComplexMappingUnfolder> unfolders, Map<OBDADataSource, ComplexMappingSQLGenerator> sqlgens) throws SQLException {
			this.predicates = predicates;
			this.sources = sources;
			this.unfolders = unfolders;
			this.sqlgens = sqlgens;

			try {
				advanceToNextPredicate();
			} catch (NoSuchElementException e) {

			}
		}

		private void advanceToNextPredicate() throws NoSuchElementException, SQLException {
			currentPredicate = predicates.next();
			currentIterator = new VirtualTripleIterator(currentPredicate, sources, unfolders, sqlgens);
		}

		@Override
		public boolean hasNext() {
			if (currentIterator == null)
				return false;

			boolean hasnext = currentIterator.hasNext();
			while (!hasnext) {
				try {
					advanceToNextPredicate();
					hasnext = currentIterator.hasNext();
				} catch (Exception e) {
					return false;
				}
			}
			return hasnext;
		}

		@Override
		public ABoxAssertion next() {
			if (currentIterator == null)
				throw new NoSuchElementException();

			boolean hasnext = currentIterator.hasNext();
			while (!hasnext) {
				try {
					advanceToNextPredicate();
					hasnext = currentIterator.hasNext();
				} catch (Exception e) {
					throw new NoSuchElementException();
				}
			}
			return currentIterator.next();
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();

		}

	}

	/***
	 * An iterator that will dynamically construct ABox assertions for the given
	 * predicate based on the results of executing the mappings for the
	 * predicate in each data source.
	 * 
	 * @author Mariano Rodriguez Muro
	 * 
	 */
	public class VirtualTripleIterator implements Iterator<ABoxAssertion> {

		/*
		 * Indicates that we have peeked to see if there are more rows and that
		 * a call to next should not invoce res.next
		 */
		private boolean										peeked			= false;

		private boolean										hasnext			= false;

		private Predicate									pred			= null;

		private DatalogProgram								query			= null;

		private Iterator<OBDADataSource>						sourceIterator	= null;

		private ResultSet									currentResults	= null;

		private LinkedList<String>							signature;

		private Map<OBDADataSource, ComplexMappingUnfolder>		unfolders;

		private Map<OBDADataSource, ComplexMappingSQLGenerator>	sqlgens;

		private Logger										log				= LoggerFactory.getLogger(VirtualTripleIterator.class);

		public VirtualTripleIterator(Predicate p, Collection<OBDADataSource> sources, Map<OBDADataSource, ComplexMappingUnfolder> unfolders,
				Map<OBDADataSource, ComplexMappingSQLGenerator> sqlgens) throws SQLException {

			this.pred = p;
			this.unfolders = unfolders;
			this.sqlgens = sqlgens;

			/*
			 * Generating the query that as for the content of this predicate
			 */
			Atom head = null;
			Atom body = null;
			signature = new LinkedList<String>();
			if (p.getArity() == 1) {
				head = obdafac.getAtom(obdafac.getPredicate(URI.create("q"), 1), obdafac.getVariable("col1"));
				body = obdafac.getAtom(p, obdafac.getVariable("col1"));
				signature.add("col1");
			} else if (p.getArity() == 2) {
				head = obdafac.getAtom(obdafac.getPredicate(URI.create("q"), 2), obdafac.getVariable("col1"), obdafac.getVariable("col2"));
				body = obdafac.getAtom(p, obdafac.getVariable("col1"), obdafac.getVariable("col2"));
				signature.add("col1");
				signature.add("col2");
			} else {
				throw new IllegalArgumentException("Invalid arity " + p.getArity() + " " + p.toString());
			}
			query = obdafac.getDatalogProgram(obdafac.getCQIE(head, body));
			sourceIterator = sources.iterator();

			try {
				advanceToNextSource();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}

		}

		/***
		 * Advances to the next sources, configures an unfolder using the
		 * mappings for the source and executes the query related to the
		 * predicate
		 * 
		 * @throws NoSuchElementException
		 * @throws Exception
		 */
		private void advanceToNextSource() throws NoSuchElementException, Exception {

			String sql = "";
			OBDADataSource source = null;

			while (sql.equals("")) {
				source = sourceIterator.next();

				ComplexMappingUnfolder unfolder = unfolders.get(source);
				ComplexMappingSQLGenerator sqlgen = sqlgens.get(source);

				DatalogProgram unfolding = unfolder.unfold(query);
				sql = sqlgen.generateSourceQuery(unfolding, signature).trim();
			}

			currentResults = jdbcMan.executeQuery(source, sql);
		}

		@Override
		public boolean hasNext() {
			try {
				if (peeked) {
					return hasnext;
				} else {

					peeked = true;
					hasnext = currentResults.next();
					while (!hasnext) {
						try {
							advanceToNextSource();
							hasnext = currentResults.next();
						} catch (NoSuchElementException e) {
							return false;
						} catch (Exception e) {
							log.error(e.getMessage());
							return false;
						}
					}
				}
				return hasnext;
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public ABoxAssertion next() {
			try {
				if (peeked) {
					peeked = false;
					return constructAssertion();
				} else {
					boolean hasnext = currentResults.next();
					while (!hasnext) {
						try {
							advanceToNextSource();
							hasnext = currentResults.next();
						} catch (NoSuchElementException e) {
							throw e;
						} catch (Exception e) {
							log.error(e.getMessage());
							throw new NoSuchElementException();
						}
					}
					return constructAssertion();
				}
			} catch (SQLException e) {
				throw new RuntimeException(e);
			}
		}

		/***
		 * Constructs an ABox assertion with the data from the current result
		 * set.
		 * 
		 * @return
		 */
		private ABoxAssertion constructAssertion() throws SQLException {
			ABoxAssertion assertion = null;
			if (pred.getArity() == 1) {
				URIConstant c = obdafac.getURIConstant(URI.create(currentResults.getString(1)));
				assertion = new DLLiterConceptABoxAssertionImpl(pred, c);
			} else if (pred.getType(1) == Predicate.COL_TYPE.OBJECT) {
				URIConstant c1 = obdafac.getURIConstant(URI.create(currentResults.getString(1)));
				URIConstant c2 = obdafac.getURIConstant(URI.create(currentResults.getString(2)));
				assertion = new DLLiterRoleABoxAssertionImpl(pred, c1, c2);
			} else {
				URIConstant c1 = obdafac.getURIConstant(URI.create(currentResults.getString(1)));
				ValueConstant c2 = obdafac.getValueConstant(currentResults.getString(2));
				assertion = new DLLiterAttributeABoxAssertionImpl(pred, c1, c2);
			}
			return assertion;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();

		}

	}
}
