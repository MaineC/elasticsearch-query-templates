/**
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.elasticsearch.index.query;

import java.io.IOException;

import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.Query;
import org.elasticsearch.cache.recycler.CacheRecyclerModule;
import org.elasticsearch.cluster.ClusterService;
import org.elasticsearch.common.inject.AbstractModule;
import org.elasticsearch.common.inject.Injector;
import org.elasticsearch.common.inject.ModulesBuilder;
import org.elasticsearch.common.inject.util.Providers;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.settings.SettingsModule;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.index.Index;
import org.elasticsearch.index.IndexNameModule;
import org.elasticsearch.index.analysis.AnalysisModule;
import org.elasticsearch.index.cache.IndexCacheModule;
import org.elasticsearch.index.codec.CodecModule;
import org.elasticsearch.index.engine.IndexEngineModule;
import org.elasticsearch.index.query.functionscore.FunctionScoreModule;
import org.elasticsearch.index.settings.IndexSettingsModule;
import org.elasticsearch.index.similarity.SimilarityModule;
import org.elasticsearch.indices.query.IndicesQueriesModule;
import org.elasticsearch.script.ScriptModule;
import org.elasticsearch.script.mustache.MustacheScriptEngineService;
import org.elasticsearch.test.ElasticsearchTestCase;
import org.elasticsearch.threadpool.ThreadPoolModule;
import org.junit.Test;

/**
 * Test parsing and executing a template request.
 * */
public class TemplateQueryParserTest extends ElasticsearchTestCase {

    @Test
    public void testParser() throws IOException {
        String templateString = "{\"template\": {"
                + "\"template_string\":\"{\\\"match_{{template}}\\\": {}}\","
                + "\"template_vars\":{\"template\":\"all\"}}" + "}";

        Settings settings = ImmutableSettings.Builder.EMPTY_SETTINGS;

        Index index = new Index("test");
        ScriptModule scriptModule = new ScriptModule(settings);
        scriptModule.addScriptEngine(MustacheScriptEngineService.class);

        Injector injector = new ModulesBuilder().add(
                new SettingsModule(settings),
                new CacheRecyclerModule(settings),
                new ThreadPoolModule(settings),
                new IndicesQueriesModule(),
                scriptModule,
                new IndexSettingsModule(index, settings),
                new IndexCacheModule(settings),
                new AnalysisModule(settings),
                new IndexEngineModule(settings),
                new SimilarityModule(settings),
                new IndexNameModule(index),
                new IndexQueryParserModule(settings),
                new CodecModule(settings),
                new FunctionScoreModule(),
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(ClusterService.class).toProvider(Providers.of((ClusterService) null));
                    }
                }
        ).createInjector();

        IndexQueryParserService queryParserService = injector.getInstance(IndexQueryParserService.class);

        XContentParser templateSourceParser = XContentFactory.xContent(templateString).createParser(templateString);
        QueryParseContext context = new QueryParseContext(index, queryParserService);
        context.reset(templateSourceParser);

        TemplateQueryParser parser = injector.getInstance(TemplateQueryParser.class);
        Query query = parser.parse(context);
        assertTrue("Parsing template query failed.", query instanceof ConstantScoreQuery);
    }
}
