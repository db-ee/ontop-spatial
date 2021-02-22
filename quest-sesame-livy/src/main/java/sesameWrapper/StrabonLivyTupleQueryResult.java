package sesameWrapper;

/*
 * #%L
 * ontop-quest-sesame
 * %%
 * Copyright (C) 2009 - 2014 Free University of Bozen-Bolzano
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import okhttp3.OkHttpClient;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.impl.MapBindingSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.stream.JsonParser;
import java.util.List;

public class StrabonLivyTupleQueryResult implements TupleQueryResult {

    JsonParser resultParser;
    private boolean nextConsumed;
    private JsonParser.Event nextResult;
    private List<String> signature;
    private List<String> tempTables; //the temp tables that have been created for this query. To delete on close
    private static final Logger log = LoggerFactory.getLogger(StrabonLivyTupleQueryResult.class);
    private String sessionUrl;

    StrabonLivyTupleQueryResult(JsonParser jsonParser, List<String> signature){
        this.resultParser = jsonParser;
        this.signature = signature;
        this.nextConsumed = false;
    }

    @Override
    public void close() throws QueryEvaluationException {
        OkHttpClient client = new OkHttpClient();
        for (int k = 0; k < tempTables.size(); k++) {
            try{
                LivyHelper.sendCommandAndPrint("DROP VIEW globaltemp."+tempTables.get(k), sessionUrl, client);
            }
            catch(Exception ex){
                log.error("Could not delete table "+tempTables.get(k)+". ");
                ex.printStackTrace();
            }
        }
        resultParser.close();
    }

    @Override
    public boolean hasNext() throws QueryEvaluationException {
        if(resultParser == null){
            return false;
        }
        if (resultParser.hasNext()){
            nextResult = resultParser.next();
            int noOfResults = 0;
            nextConsumed = true;
            if (nextResult == JsonParser.Event.END_ARRAY) {
                return false;
            }
            else{
                return true;
            }
        }
        else{
            return false;
        }
    }

    @Override
    public BindingSet next() throws QueryEvaluationException {
        if(!nextConsumed) {
            nextResult = resultParser.next();
        }
        nextConsumed = false;
        DocumentContext tuple = JsonPath.parse(resultParser.getString());

        MapBindingSet set = new MapBindingSet(this.signature.size() * 2); //why *2?
        for (String name : this.signature) {
            Binding binding = createBinding(name, tuple, this.signature);
            if (binding != null) {
                set.addBinding(binding);
            }
        }
        return set;
    }

    @Override
    public void remove() throws QueryEvaluationException {
        throw new QueryEvaluationException("The query result is read-only. Elements cannot be removed");
    }


    private Binding createBinding(String bindingName, DocumentContext tuple, List<String> signature2) {
        StrabonLivyBindingSet bset = new StrabonLivyBindingSet(tuple, signature2);
        return bset.getBinding(bindingName);
    }


    @Override
    public List<String> getBindingNames() throws QueryEvaluationException {
        return this.signature;
    }
    

    public void setTempTables(List<String> temp) {
        this.tempTables = temp;
    }


    public void setSessionUrl(String sessionUrl) {
        this.sessionUrl = sessionUrl;
    }
}
