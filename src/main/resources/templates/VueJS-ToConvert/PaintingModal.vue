<template>
  <div class="modal-container">
    <div class="close" @click="close">&times;</div>
    <div class="body">
      <div class="modal">
        <!-- slot='modal' -->
        <img :src="imageurl" :class="`img-index--${id}`" />
        <p class="card_text">{{ caption }}</p>
        <p class="card_text">{{ dimensions }}</p>
        <p class="card_text">{{ medium }}</p>
        <p v-if="sold" class="card_text">Sold!</p>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: "PaintingModal",
  props: {
    id: Number,
    imageurl: String,
    caption: String,
    dimensions: String,
    medium: String,
    sold: Boolean,
  },
  methods: {
    close() {
      this.$emit("close");
    },
  },
};
</script>

<style scoped>
.modal-container {
  background-color: rgba(0, 0, 0, 0.8);
  position: fixed;
  top: 0;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 1050;
  padding: 50px;
}

.modal-container .body {
  /* background-color: #fff; */
}
.modal-container .close {
  right: 5px;
  position: relative;
  color: white;
  font-size: 30px;
  text-align: right;
  top: 30px;
  cursor: pointer;
}
.close {
  text-shadow: 4px 4px 3px black;
}
.modal {
  /* display: block; */
  /* margin: 0 auto; */
}
.modal img {
  display: inline-block;
  max-width: 100%;
  box-shadow: 8px 8px 25px black;
}
.modal p {
  display: block;
  color: white;
  text-align: center;
  font-size: 1.3rem;
  width: fit-content;
  text-shadow: 4px 4px 3px black;
  /* background-color: rgba(255, 255, 255, 0.2); */
}
</style>
