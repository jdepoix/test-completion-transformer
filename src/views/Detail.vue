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
        
        <div class="col-12 mb-4" v-if="testFile">
          <div class="accordion" id="testFileAccordion">
            <div class="card">
              <div class="card-header p-1 clickable">
                <button class="btn btn-block text-left font-technical" type="button" data-toggle="collapse" data-target="#collapseTestFile">
                  {{ `${testRelation.test_package}.${testRelation.test_class}.${testRelation.test_method}` }}
                </button>
              </div>
              <div id="collapseTestFile" class="collapse show code-card" data-parent="#testFileAccordion">
                <div class="card-body p-0" >
                  <pre v-highlight-syntax class="m-0"><code class="java" v-html="testFile"></code></pre>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="col-12 mb-4" v-if="testRelation.gwt_resolution_status === 'RESOLVED'">
          <div class="card">
            <div class="card-body" v-if="context">
              <h5>Context</h5>
              <div class="accordion" id="contextAccordion">
                <div class="card" v-for="contextItem in context" :key="contextItem.id">
                  <div class="card-header p-1 clickable">
                    <button class="btn btn-block text-left font-technical" type="button" data-toggle="collapse" :data-target="`#collapseContext${contextItem.id}`">
                      <span v-if="contextItem.resolution_status === 'RESOLVED'" class="badge badge-success">RESOLVED</span>
                      <span v-else class="badge badge-danger">UNRESOLVABLE</span>
                      {{ contextItem.resolution_status === 'RESOLVED' ? `${contextItem.package}.${contextItem.class}.${contextItem.method}` : contextItem.method_call }}
                    </button>
                  </div>

                  <div :id="`collapseContext${contextItem.id}`" class="collapse" data-parent="#contextAccordion" v-if="contextItem.resolution_status === 'RESOLVED'">
                    <div class="card-body code-card p-0" v-if="contextItem.path">
                      <pre v-highlight-syntax class="m-0" v-if="contextItem.fileContent"><code class="java">{{ contextItem.fileContent }}</code></pre>
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
              <pre v-highlight-syntax class="m-0"><code class="java" v-html="highlightRelevantCodePart(testRelation.given_section, 'WHEN')"></code></pre>
            </div>
            <div class="card-body">
              <h5>When</h5>
              <pre v-highlight-syntax class="m-0"><code class="java">{{ testRelation.related_method }}</code></pre>
            </div>
            <div class="card-body">
              <h5>Then</h5>
              <pre v-highlight-syntax class="m-0"><code class="java" v-html="this.highlightRelevantCodePart(testRelation.then_section, 'WHEN')">testRelation.then_section</code></pre>
            </div>
          </div>
        </div>
        <div class="col-12 mb-4" v-if="relatedFile">
          <div class="accordion" id="relatedFileAccordion">
            <div class="card">
              <div class="card-header p-1 clickable">
                <button class="btn btn-block text-left font-technical" type="button" data-toggle="collapse" data-target="#collapseRelatedFile">
                  {{ `${testRelation.related_package}.${testRelation.related_class}.${testRelation.related_method}` }}
                </button>
              </div>
              <div id="collapseRelatedFile" class="collapse show code-card" data-parent="#relatedFileAccordion">
                <div class="card-body p-0" >
                  <pre v-highlight-syntax class="m-0"><code class="java" v-html="relatedFile"></code></pre>
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
import HighlightSyntax from '../directives/highlight-syntax.js';

export default {
  name: 'Detail',
  data: () => ({
    testRelation: null,
    testFile: null,
    relatedFile: null,
    context: null,
  }),
  directives: {
    HighlightSyntax: new HighlightSyntax()
  },
  mounted() {
    this.loadTestRelation();
  },
  updated() {
    this.scrollHighlightedMethodIntoView(document.getElementById("collapseTestFile"));
    this.scrollHighlightedMethodIntoView(document.getElementById("collapseRelatedFile"));
  },
  methods: {
    loadTestRelation() {
      testRelationApi.getRepo(this.$route.params.id).then(result => {
        this.testRelation = result;
        if (this.testRelation.when_section) {
          this.testRelation.when_section = this.testRelation.when_section.replace(`.${this.testRelation.related_method}(`, '.<WHEN>(')
        }
        if (this.testRelation.given_section) {
          this.testRelation.given_section = this.testRelation.given_section.replace(`.${this.testRelation.related_method}(`, '.<WHEN>(')
        }
        this._loadFiles();
        this._loadContext();
      })
    },
    _loadFiles() {
      this.getHighlightedFileContent(`${this.testRelation.repo_name}/${this.testRelation.test_file_path}`).then(
        fileContent => {
          this.testFile = this.highlightRelevantCodePart(fileContent, this.testRelation.test_method_signature);
        }
      );
      if (this.testRelation.related_file_path) {
        this.getHighlightedFileContent(`${this.testRelation.repo_name}/${this.testRelation.related_file_path}`).then(
          fileContent => {
            this.relatedFile = this.highlightRelevantCodePart(fileContent, this.testRelation.related_method_signature);
          }
        );
      }
    },
    _loadContext() {
      testRelationApi.getContext(this.testRelation.id).then(context => {
        this.context = [];
        let signatures = {};
        context.forEach(contextItem => {
          let signature = `${contextItem.package}.${contextItem.class}.${contextItem.method}`;
          if (!(signature in signatures)) {
            contextItem["id"] = hash(contextItem);
            contextItem["fileContent"] = null;
            if (contextItem.path) {
              this.getContextFile(contextItem);
            }
            this.context.push(contextItem);
            signatures[signature] = true;
          }
        });
      });
    },
    scrollHighlightedMethodIntoView(parent) {
      if (parent) {
        parent.getElementsByClassName('highlight-method').forEach(element => parent.scrollTop = element.offsetTop - 70);
      }
    },
    async getHighlightedFileContent(filepath) {
      return await repoFileApi.getFileContent(filepath);
    },
    highlightRelevantCodePart(code, highlightString) {
      return code.replace(highlightString, `<mark class="highlight-method">${highlightString}</mark>`);
    },
    getContextFile(contextObject) {
      repoFileApi.getFileContent(`${this.testRelation.repo_name}/${contextObject.path}`).then(fileContent => contextObject.fileContent = fileContent);
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
