<template>
  <div class="exhibition-container">
    <h2 class="title">{{ title }}</h2>
    <section class="page-details">
      <img :src="image" :alt="title" class="inset-image" />
      <p class="info">{{ date }}</p>
      <p v-if="!addressIsLink" class="info">{{ address }}</p>
      <a v-if="addressIsLink" :href="address" class="info">{{ address }}</a>
      <div class="content-text" v-html="content"></div>
      <!-- v-if="exhibition.content"-->
    </section>
  </div>
</template>

<script>
export default {
  name: "TheExhibition",
  props: {
    title: String,
    date: String,
    address: String,
    content: String,
    image: String,
  },
  computed: {
    addressIsLink() {
      if (this.address.includes("http")) {
        return true;
      } else return false;
    },
  },
};
</script>

<style scoped>
.title {
  /* position: relative; */
  text-align: left;
  font-size: 20pt;
  /* padding-left: 5%; */
  /* margin: 0; */
  margin: 0 auto;
  /* margin-left: 18%; */
  /* float: left; */
}
.info a,
.info {
  /* padding-left: 5%; */
  margin: 0 auto;
  text-align: justify;
  width: 90%;
  margin: 0 0;
  font-style: italic;
}
.inset-image {
  float: right;
  width: 30%;
  padding: 2%;
  max-width: 100%;
}
.content-text {
  text-align: justify;
  width: 90%;
  margin: 0 auto;
}
</style>
