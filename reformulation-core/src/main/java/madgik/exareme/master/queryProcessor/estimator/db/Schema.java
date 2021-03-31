package madgik.exareme.master.queryProcessor.estimator.db;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import madgik.exareme.master.queryProcessor.analyzer.stat.JoinCardinalities;

import java.util.HashMap;
import java.util.Map;

/**
 * @author jim
 */
public class Schema {

	private String schemaName;
	private Map<Integer, RelInfo> tableIndex;
	private JoinCardinalities cards;
	private Map<String, Long> unionCards;

	/* constructor */
	public Schema(String schemaName, Map<Integer, RelInfo> relIndex) {
		this.schemaName = schemaName;
		this.tableIndex = relIndex;
	}

	/* copy constructor */
	public Schema(Schema schema) {
		this.schemaName = schema.getSchemaName();
		this.tableIndex = new HashMap<Integer, RelInfo>();

		for (Map.Entry<Integer, RelInfo> entry : schema.tableIndex.entrySet()) {
			this.tableIndex.put(entry.getKey(), new RelInfo(entry.getValue()));
		}

	}

	/* getters and setters */
	public String getSchemaName() {
		return schemaName;
	}

	public Map<Integer, RelInfo> getTableIndex() {
		return tableIndex;
	}

	/* interface methods */

	/* standard methods */
	@Override
	public String toString() {
		return "Schema{" + "schemaName=" + schemaName + ", tableIndex=" + tableIndex + '}';
	}

	public JoinCardinalities getCards() {
		return cards;
	}

	public void setCards(JoinCardinalities cards) {
		this.cards = cards;
	}

	public Map<String, Long> getUnionCards() {
		return unionCards;
	}

	public void setUnionCards(Map<String, Long> unionCards) {
		this.unionCards = unionCards;
	}
	
	

}
