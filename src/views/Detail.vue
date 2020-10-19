<template>
  <div class="details">
    <h1 class="mt-5 mb-5">Data Explorer</h1>

    <div v-if="testRelation === null">
      Loading...
    </div>
    <div v-else>
      <div class="row">
        <div class="col-12 mb-4">
          <div class="card">
            <div class="card-body">
              <div class="row">
                <div class="col-12"><span class="font-weight-bold">ID:</span> <span class="font-technical">{{ testRelation.id }}</span></div>
                <div class="col-6"><span class="font-weight-bold">Repo:</span> <span class="font-technical">{{ testRelation.repo_name }}</span></div>
                <div class="col-6"><span class="font-weight-bold">Relation Type:</span> <span class="font-technical">{{ testRelation.relation_type }}</span></div>
                <div class="col-6"><span class="font-weight-bold">Resolution Status:</span> <span class="font-technical">{{ testRelation.resolution_status }}</span></div>
                <div class="col-6"><span class="font-weight-bold">GWT Resolution Status:</span> <span class="font-technical">{{ testRelation.gwt_resolution_status }}</span></div>
              </div>
            </div>
          </div>
        </div>

        <div class="col-12 mb-4" v-if="relatedFile">
          <h2>Code under test</h2>
          <div class="accordion" id="relatedFileAccordion">
            <div class="card">
              <div class="card-header p-1 clickable">
                <button class="btn btn-block text-left font-technical" type="button" data-toggle="collapse" data-target="#collapseRelatedFile" @click="scrollHighlightedMethodIntoView('collapseRelatedFile')">
                  {{ `${testRelation.related_package}.${testRelation.related_class}.${testRelation.related_method}` }}
                </button>
              </div>
              <div id="collapseRelatedFile" class="collapse code-card" data-parent="#relatedFileAccordion">
                <div class="card-body p-0" >
                  <pre v-highlight-syntax class="m-0"><code class="java" v-html="relatedFile"></code></pre>
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <div class="col-12 mb-4" v-if="testFile">
          <h2>Test code</h2>
          <div class="accordion" id="testFileAccordion">
            <div class="card">
              <div class="card-header p-1 clickable">
                <button class="btn btn-block text-left font-technical" type="button" data-toggle="collapse" data-target="#collapseTestFile" @click="scrollHighlightedMethodIntoView('collapseTestFile')">
                  {{ `${testRelation.test_package}.${testRelation.test_class}.${testRelation.test_method}` }}
                </button>
              </div>
              <div id="collapseTestFile" class="collapse code-card" data-parent="#testFileAccordion">
                <div class="card-body p-0" >
                  <pre v-highlight-syntax class="m-0"><code class="java" v-html="testFile"></code></pre>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="col-12 mb-4" v-if="testRelation.gwt_resolution_status === 'RESOLVED'">
          <div class="card">
            <div class="card-body" v-if="context === null">
              <h5>Context</h5>
              <span class="font-technical">LOADING ...</span>
            </div>
            <div class="card-body" v-else-if="context.length">
              <h5>Context</h5>
              <div class="accordion" id="contextAccordion">
                <div class="card" v-for="contextItem in context" :key="contextItem.id">
                  <div class="card-header p-1 clickable">
                    <button class="btn btn-block text-left font-technical" type="button" data-toggle="collapse" :data-target="`#collapseContext${contextItem.id}`" @click="scrollHighlightedMethodIntoView(`collapseContext${contextItem.id}`)">
                      <span v-if="contextItem.resolution_status === 'RESOLVED'" class="badge badge-success">RESOLVED</span>
                      <span v-else class="badge badge-danger">UNRESOLVABLE</span>
                      {{ contextItem.resolution_status === 'RESOLVED' ? `${contextItem.package}.${contextItem.class}.${contextItem.method}` : contextItem.method_call }}
                    </button>
                  </div>

                  <div :id="`collapseContext${contextItem.id}`" class="collapse code-card" data-parent="#contextAccordion" v-if="contextItem.resolution_status === 'RESOLVED'">
                    <div class="card-body p-0" v-if="contextItem.path">
                      <pre v-highlight-syntax class="m-0" v-if="contextItem.fileContent"><code class="java" v-html="highlightRelevantCodePart(contextItem.fileContent, contextItem.method_signature)"></code></pre>
                      <span v-else>LOADING ...</span>
                    </div>
                    <div class="card-body code-card p-0" v-else>
                      <pre v-highlight-syntax class="m-0"><code class="java">/* INTERNAL */</code></pre>
                    </div>
                  </div>
                </div>
              </div>
            </div>
            <div class="card-body">
              <h5>Given</h5>
              <pre v-highlight-syntax class="m-0"><code class="java" v-html="highlightRelevantCodePart(testRelation.substituted_given_section, 'WHEN')"></code></pre>
            </div>
            <div class="card-body">
              <h5>When</h5>
              <pre v-highlight-syntax class="m-0"><code class="java">{{ testRelation.related_method }}</code></pre>
            </div>
            <div class="card-body">
              <h5>Then</h5>
              <pre v-highlight-syntax class="m-0"><code class="java" v-html="highlightRelevantCodePart(testRelation.substituted_then_section, 'WHEN')"></code></pre>
            </div>
          </div>
        </div>

        <div class="col-12 mb-4" v-if="testRelation.gwt_resolution_status === 'RESOLVED'">
          <h2>Predictions</h2>
          <div class="accordion" id="slmPredictionAccordion">
            <div class="card">
              <div class="card-header p-1 clickable">
                <button class="btn btn-block text-left font-weight-bold" type="button" data-toggle="collapse" data-target="#collapseSlmPredictionAccordion" @click="loadSlmPrediction()">
                  Structured Language Model
                </button>
              </div>
              <div id="collapseSlmPredictionAccordion" class="collapse" data-parent="#slmPredictionAccordion">
                <div class="card-body">
                  <span class="font-technical" v-if="testRelation.then_section.includes(`${this.testRelation.related_method}(`)">WHEN call in THEN section!</span>
                  <ul class="list-group" v-else-if="slmPrediction && slmPrediction.length !== 0">
                    <li class="list-group-item font-technical" v-for="prediction in slmPrediction[0]" :key="prediction.code">{{ prediction.prob }} - {{ prediction.code }}</li>
                  </ul>
                  <span class="font-technical" v-else>LOADING ...</span>
                </div>
              </div>
            </div>
          </div>

          <div class="accordion" id="transformerPredictionAccordion" v-if="testFileRaw !== null && relatedFileRaw !== null">
            <div class="card">
              <div class="card-header p-1 clickable">
                <button class="btn btn-block text-left font-weight-bold" type="button" data-toggle="collapse" data-target="#collapseTransformerPredictionAccordion" @click="loadTransformerPrediction()">
                  Transformer
                </button>
              </div>
              <div id="collapseTransformerPredictionAccordion" class="collapse" data-parent="#transformerPredictionAccordion">
                <div class="card-body" v-if="testRelation.then_section.includes(`${this.testRelation.related_method}(`)">
                  <span class="font-technical">WHEN call in THEN section!</span>
                </div>
                <div class="card-body p-0" v-else-if="transformerPrediction != null && transformerPrediction.status == 'SUCCESS'">
                  <pre v-highlight-syntax class="m-0"><code class="java" v-html="transformerPrediction.data"></code></pre>
                </div>
                <div class="card-body" v-else-if="transformerPrediction != null">
                  <span class="font-technical">Prediction failed with {{ transformerPrediction.data }}</span>
                </div>
                <div class="card-body" v-else>
                  <span class="font-technical">LOADING ...</span>
                </div>
              </div>
            </div>
          </div>
        </div>
        
      </div>
    </div>
  </div>
</template>

<script>
import hash from 'object-hash';
import testRelationApi from '../core/test-relation-api.js';
import repoFileApi from '../core/repo-file-api';
import slmPredictionApi from '../core/slm-prediction-api';
import transformerPredictionApi from '../core/transformer-prediction-api';
import HighlightSyntax from '../directives/highlight-syntax.js';

export default {
  name: 'Detail',
  data: () => ({
    testRelation: null,
    testFile: null,
    testFileRaw: null,
    relatedFile: null,
    relatedFileRaw: null,
    context: null,
    slmPrediction: null,
    transformerPrediction: null,
  }),
  directives: {
    HighlightSyntax: new HighlightSyntax()
  },
  mounted() {
    this.loadTestRelation();
  },
  methods: {
    loadTestRelation() {
      testRelationApi.getRepo(this.$route.params.id).then(result => {
        this.testRelation = result;
        if (this.testRelation.given_section) {
          this.testRelation.substituted_given_section = this.testRelation.given_section.split(`${this.testRelation.related_method}(`).join('<WHEN>(');
        }
        if (this.testRelation.then_section) {
          this.testRelation.substituted_then_section = this.testRelation.then_section.split(`${this.testRelation.related_method}(`).join('<WHEN>(');
        }
        this._loadFiles();
        this._loadContext();
      })
    },
    _loadFiles() {
      this.getHighlightedFileContent(`${this.testRelation.repo_name}/${this.testRelation.test_file_path}`).then(
        fileContent => {
          this.testFileRaw = fileContent;
          this.testFile = this.highlightRelevantCodePart(fileContent, this.testRelation.test_method_signature.trim());
        }
      );
      if (this.testRelation.related_file_path) {
        this.getHighlightedFileContent(`${this.testRelation.repo_name}/${this.testRelation.related_file_path}`).then(
          fileContent => {
            this.relatedFileRaw = fileContent;
            this.relatedFile = this.highlightRelevantCodePart(fileContent, this.testRelation.related_method_signature.trim());
          }
        );
      }
    },
    _loadContext() {
      testRelationApi.getContext(this.testRelation.id).then(context => {
        let tempContext = [];
        let signatures = {};
        context.forEach(contextItem => {
          let signature = `${contextItem.package}.${contextItem.class}.${contextItem.method}`;
          if (contextItem.path === null || !(signature in signatures)) {
            contextItem["id"] = hash(contextItem);
            contextItem["fileContent"] = null;
            if (contextItem.path) {
              this.getContextFile(contextItem);
            }
            tempContext.push(contextItem);
            signatures[signature] = true;
          }
        });
        this.context = tempContext;
      });
    },
    scrollHighlightedMethodIntoView(parentId) {
      setTimeout(() => {
        let parent = document.getElementById(parentId);
        if (parent) {
          parent.getElementsByClassName('highlight-method').forEach(element => parent.scrollTop = element.offsetTop - 70);
        }
      }, 1);
    },
    async getHighlightedFileContent(filepath) {
      return await repoFileApi.getFileContent(filepath);
    },
    highlightRelevantCodePart(code, highlightString) {
      return code.split(highlightString).join(`<mark class="highlight-method">${highlightString}</mark>`);
    },
    getContextFile(contextObject) {
      repoFileApi.getFileContent(`${this.testRelation.repo_name}/${contextObject.path}`).then(fileContent => contextObject.fileContent = fileContent);
    },
    loadSlmPrediction() {
      if (!this.slmPrediction && !this.testRelation.then_section.includes(`${this.testRelation.related_method}(`)) {
        this.slmPrediction = [];
        slmPredictionApi
          .getPrediction(`@Test public void ${this.testRelation.test_method}() { ${this.testRelation.given_section.split('\n').join(' ')} ?? }`)
          .then(
            response => this.slmPrediction = response
          );
      }
    },
    loadTransformerPrediction() {
      if (!this.transformerPrediction && !this.testRelation.then_section.includes(`${this.testRelation.related_method}(`)) {
        transformerPredictionApi.getPrediction(
          'default',
          this.testFileRaw,
          this.testRelation.test_class,
          this.testRelation.test_method_signature,
          this.relatedFileRaw,
          this.testRelation.related_class,
          this.testRelation.related_method_signature,
          this.testRelation.then_section_start_index,
        ).then(
          response => {
            this.transformerPrediction = response;
          }
        );
      }
    }
  }
}
</script>

<style>
  .highlight-method {
    background: yellow;
    color: gray;
  }
  .highlight-method code {
    background: inherit;
    color: inherit;
  }
  .code-card {
    background-color: #f8f8f8;
    max-height: 500px;
    overflow: auto;
  }
</style>
