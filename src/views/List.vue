<template>
  <div class="list">
    <h1 class="mt-5">Data Explorer</h1>
    <div class="input-group mb-3 mt-5">
      <input v-model="searchValue" type="text" class="form-control" placeholder="Repo name" aria-labzel="Repo name" aria-describedby="search-repo">
      <div class="input-group-append">
        <button class="btn btn-secondary" type="button" id="search-repo" @click="loadRepos()">Filter</button>
      </div>
      <router-link :to="'/test-relation/random'">
        <button type="button" class="btn btn-primary ml-2">Get random</button>
      </router-link>
    </div>
    <ul class="list-group">
      <li v-for="result in searchResults" :key="result.id" class="list-group-item">
        <router-link :to="'/test-relation/' + result.id">
          {{ result.repo_name }} - {{ result.test_package }}.{{ result.test_class }}.{{ result.test_method }}
        </router-link>
        <span v-if="result.resolution_status === 'RESOLVED'" class="badge badge-success float-right ml-1">RESOVLED</span>
        <span v-if="result.gwt_resolution_status === 'RESOLVED'" class="badge badge-success float-right ml-1">GWT RESOVLED</span>
      </li>
    </ul>
    <nav class="mt-3">
      <ul class="pagination justify-content-center">
        <li class="page-item clickable" :class="{'disabled': page <= 1}" @click="previousPage()"><a class="page-link">Previous</a></li>
        <li class="page-item clickable" @click="nextPage()"><a class="page-link">Next</a></li>
      </ul>
    </nav>
  </div>
</template>

<script>
import testRelationApi from '../core/test-relation-api.js'

export default {
  name: 'List',
  data: () => ({
    searchValue: "",
    page: 1,
    searchResults: []
  }),
  methods: {
    loadRepos() {
      testRelationApi.listRepos(this.page, this.searchValue).then(
        result => {
          this.searchResults = result
        }
      );
    },
    nextPage() {
      this.page++;
      this.loadRepos();
    },
    previousPage() {
      this.page--;
      this.loadRepos();
    }
  },
  mounted() {
    this.loadRepos();
  },
}
</script>

<style>
.clickable {
  cursor: pointer;
}
</style>