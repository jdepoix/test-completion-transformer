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
                <div class="col-4"><span class="font-weight-bold">Relation Type:</span> <span class="font-technical">{{ testRelation.relation_type }}</span></div>
                <div class="col-4"><span class="font-weight-bold">Resolution Status:</span> <span class="font-technical">{{ testRelation.resolution_status }}</span></div>
                <div class="col-4"><span class="font-weight-bold">GWT Resolution Status:</span> <span class="font-technical">{{ testRelation.gwt_resolution_status }}</span></div>
              </div>
            </div>
          </div>
        </div>
        
        <div class="col-12 mb-4" v-if="testFile">
          <div class="card code-card" id="testFileCodeCard">
            <div class="card-body">
              <pre v-highlight-syntax class="m-0"><code class="java" v-html="testFile"></code></pre>
            </div>
          </div>
        </div>

        <div class="col-12 mb-4" v-if="testRelation.gwt_resolution_status === 'RESOLVED'">
          <div class="card">
            <div class="card-body">
              <h5>Given</h5>
              <pre v-highlight-syntax class="m-0"><code class="java" v-html="highlightRelevantCodePart(testRelation.given_section, '<WHEN>')"></code></pre>
            </div>
            <div class="card-body">
              <h5>When</h5>
              <pre v-highlight-syntax class="m-0"><code class="java">{{ testRelation.when_section }}</code></pre>
            </div>
            <div class="card-body">
              <h5>Then</h5>
              <pre v-highlight-syntax class="m-0"><code class="java" v-html="this.highlightRelevantCodePart(testRelation.then_section, 'WHEN')">testRelation.then_section</code></pre>
            </div>
          </div>
        </div>

        <div class="col-12 mb-4" v-if="relatedFile">
          <div class="card code-card" id="relatedFileCodeCard">
            <div class="card-body">
              <pre v-highlight-syntax class="m-0"><code class="java" v-html="relatedFile"></code></pre>
            </div>
          </div>
        </div>

      </div>
    </div>
  </div>
</template>

<script>
import testRelationApi from '../core/test-relation-api.js';
import repoFileApi from '../core/repo-file-api';
import HighlightSyntax from '../directives/highlight-syntax.js';

export default {
  name: 'Detail',
  data: () => ({
    testRelation: null,
    testFile: null,
    relatedFile: null
  }),
  directives: {
    HighlightSyntax: new HighlightSyntax()
  },
  mounted() {
    this.loadTestRelation();
  },
  updated() {
    this.scrollHighlightedMethodIntoView(document.getElementById("testFileCodeCard"));
    this.scrollHighlightedMethodIntoView(document.getElementById("relatedFileCodeCard"));
  },
  methods: {
    loadTestRelation() {
      testRelationApi.getRepo(this.$route.params.id).then(result => {
        this.testRelation = result;
        this._loadFiles();
      })
    },
    _loadFiles() {
      this.getHighlightedFileContent(`${this.testRelation.repo_name}/${this.testRelation.test_file_path}`).then(
        fileContent => {
          this.testFile = this.highlightRelevantCodePart(fileContent, this.testRelation.test_method);
        }
      );
      if (this.testRelation.related_file_path) {
        this.getHighlightedFileContent(`${this.testRelation.repo_name}/${this.testRelation.related_file_path}`).then(
          fileContent => {
            this.relatedFile = this.highlightRelevantCodePart(fileContent, this.testRelation.related_method);
          }
        );
      }
    },
    scrollHighlightedMethodIntoView(parent) {
      if (parent) {
        parent.getElementsByClassName('highlight-method').forEach(element => parent.scrollTop = element.offsetTop);
      }
    },
    async getHighlightedFileContent(filepath) {
      return await repoFileApi.getFileContent(filepath);
    },
    highlightRelevantCodePart(code, highlightString) {
      return code.replace(highlightString, `<mark class="highlight-method">${highlightString}</mark>`);
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
