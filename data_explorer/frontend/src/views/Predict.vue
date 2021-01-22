<template>
  <div class="predict">
    <div class="row">
      <div class="col-12 mt-5 mb-5">
        <h1 class="">Prediction Explorer</h1>
      </div>

      <div class="col-12">
        <h2>Code under test</h2>
      </div>
      <div class="col-12 mb-1">
        <prism-editor class="editor pt-3 pb-3" :highlight="highlighter" v-model="testedCode" line-numbers :tab-size="4"></prism-editor>
      </div>
      <div class="col-6 mb-5 pr-0">
        <div class="input-group">
          <div class="input-group-prepend">
            <span class="input-group-text font-weight-bold">Class</span>
          </div>
          <prism-editor class="editor form-control" :highlight="highlighter" v-model="testedClass" :tab-size="4"></prism-editor>
        </div>
      </div>
      <div class="col-6 mb-5 pl-1">
        <div class="input-group">
          <div class="input-group-prepend">
            <span class="input-group-text font-weight-bold">Signature</span>
          </div>
          <prism-editor class="editor form-control" :highlight="highlighter" v-model="testedSignature" :tab-size="4"></prism-editor>
        </div>
      </div>
        
      <div class="col-12">
        <h2>Test code</h2>
      </div>
      <div class="col-12 mb-1">
        <prism-editor class="editor pt-3 pb-3" :highlight="highlighter" v-model="testCode" line-numbers :tab-size="4"></prism-editor>
      </div>
      <div class="col-6 mb-5 pr-0">
        <div class="input-group">
          <div class="input-group-prepend">
            <span class="input-group-text font-weight-bold">Class</span>
          </div>
          <prism-editor class="editor form-control" :highlight="highlighter" v-model="testClass" :tab-size="4"></prism-editor>
        </div>
      </div>
      <div class="col-6 mb-5 pl-1">
        <div class="input-group">
          <div class="input-group-prepend">
            <span class="input-group-text font-weight-bold">Signature</span>
          </div>
          <prism-editor class="editor form-control" :highlight="highlighter" v-model="testSignature" :tab-size="4"></prism-editor>
        </div>
      </div>

      <div class="col-12">
        <h2>Prediction</h2>
      </div>
      <div class="col-12 mb-1">
        <div class="input-group input-group-lg">
          <div class="input-group-prepend">
            <label class="input-group-text" for="inputGroupSelect01">Model</label>
          </div>
          <select v-model="model" class="custom-select">
            <option disabled>Select model...</option>
            <option value="default">Transformer</option>
          </select>
          <select v-model="sampler" class="custom-select">
            <option disabled>Select sampler...</option>
            <option value="NUCLEUS">Nucleus</option>
            <option value="ONLY_KNOWN_IDENTIFIERS_NUCLEUS">Nucleus (favor known identifiers)</option>
            <option value="GREEDY">Greedy</option>
            <option value="ONLY_KNOWN_IDENTIFIERS_GREEDY">Greedy (favor known identifiers)</option>
          </select>
          <div class="input-group-append">
            <button 
              @click="runPrediction" 
              id="predictButton"
              class="btn btn-primary" 
              type="button" 
              :disabled="loading || testedCode == '' || testCode == '' || testedSignature == '' || testSignature == ''"
            >
              <span v-if="!loading">PREDICT</span>
              <div v-else class="spinner-border" role="status">
                <span class="sr-only">Loading...</span>
              </div>
            </button>
          </div>
        </div>
      </div>
      <div class="col-12 mb-5" v-if="prediction">
        <prism-editor v-if="prediction.status == 'SUCCESS'" class="editor pt-3 pb-3" :highlight="highlighter" v-model="prediction.data" line-numbers readonly :tab-size="4"></prism-editor>
        <div v-else class="editor p-3">
          Prediction failed with "{{ prediction.data }}"
        </div>
      </div>
    </div>
  </div>
</template>
<script>
import { PrismEditor } from 'vue-prism-editor';
import 'vue-prism-editor/dist/prismeditor.min.css';

import { highlight, languages } from 'prismjs/components/prism-core';
import 'prismjs/components/prism-clike';
import 'prismjs/components/prism-java';
import 'prismjs/themes/prism.css';

import transformerPredictionApi from '../core/transformer-prediction-api';

const testedClassTemplate  = `public class TestedClass {
    public void testedMethod() {
        // ...
    }
}`;
const testClassTemplate = `public class TestClass {
    @Test
    public void testMethod() {
        TestedClass testedClass = new TestedClass();
        // ...
    }
}`;

export default {
  name: 'Predict',
  components: {
    PrismEditor,
  },
  data: () => ({
    testedCode: testedClassTemplate,
    testedClass: 'TestedClass',
    testedSignature: 'public void testedMethod()',
    testCode: testClassTemplate,
    testClass: 'TestClass',
    testSignature: 'public void testMethod()',
    prediction: null,
    loading: false,
    sampler: 'NUCLEUS',
    model: 'default',
  }),
  methods: {
    highlighter(code) {
      return highlight(code, languages.java);
    },
    runPrediction() {
      this.prediction = null;
      this.loading = true;
      transformerPredictionApi.getPrediction(
        this.model,
        this.testCode,
        this.testClass,
        this.testSignature,
        this.testedCode,
        this.testedClass,
        this.testedSignature,
        null,
        this.sampler,
      ).then(
        response => {
          this.prediction = response;
          this.loading = false;
        }
      );
    },
    saveStateToUrl() {
      const encodedState = encodeURIComponent(JSON.stringify({
        testedCode: this.testedCode,
        testedClass: this.testedClass,
        testedSignature: this.testedSignature,
        testCode: this.testCode,
        testClass: this.testClass,
        testSignature: this.testSignature,
        prediction: this.prediction,
        sampler: this.sampler,
        model: this.model,
      }));
      history.pushState({}, null, `?state=${encodedState}`);
    },
    loadStateFromUrl() {
      const param = new URLSearchParams(window.location.search).get('state');
      if (param === null) {
        return;
      }
      const previousState = JSON.parse(param);
      this.testedCode = previousState.testedCode;
      this.testedClass = previousState.testedClass;
      this.testedSignature = previousState.testedSignature;
      this.testCode = previousState.testCode;
      this.testClass = previousState.testClass;
      this.testSignature = previousState.testSignature;
      this.prediction = previousState.prediction;
      this.sampler = previousState.sampler;
      this.model = previousState.model;
    }
  },
  watch: {
    testedCode() {
      this.saveStateToUrl();
    },
    testedClass() {
      this.saveStateToUrl();
    },
    testCode() {
      this.saveStateToUrl();
    },
    testClass() {
      this.saveStateToUrl();
    },
    prediction() {
      this.saveStateToUrl();
    },
    sampler() {
      this.saveStateToUrl();
    },
    model() {
      this.saveStateToUrl();
    },
    testedSignature() {
      if (this.testedSignature.includes('\n')) {
        this.testedSignature = this.testedSignature.replace('\n', '');
      }
      this.saveStateToUrl();
    },
    testSignature() {
      if (this.testSignature.includes('\n')) {
        this.testSignature = this.testSignature.replace('\n', '');
      }
      this.saveStateToUrl();
    },
  },
  mounted() {
    this.loadStateFromUrl();
  }
}
</script>

<style>
.editor {
  border: 1px solid rgba(0,0,0,.125);
  border-radius: .25rem;
  background: #f8f8f8;
  font-family: Fira code, Fira Mono, Consolas, Menlo, Courier, monospace;
}

.fixed-size {
  min-height: 200px;
  max-height: 600px;
}

.prism-editor__textarea:focus {
  outline: none;
}

#predictButton {
  width: 150px;
  height: 48px;
}
</style>