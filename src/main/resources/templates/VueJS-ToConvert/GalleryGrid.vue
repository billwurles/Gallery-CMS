<template>
  <div>
    <section v-if="paintings" class="grid">
      <div v-for="painting in paintings" :key="painting.id" class="painting">
        <!-- div @click="imgClick(painting)" style="cursor: pointer">
          <img
            :src="
              'https://edit.arabellaharcourtcooze.co.uk' +
              painting.Photo.data.attributes.formats.medium.url
            "
            :alt="painting.Description"
          />
          <p class="caption">{{ painting.Name }}</p>
        </div>
        <PaintingModal
          v-if="painting.show"
          @close="painting.show = false"
          :id="painting.id"
          :imageurl="
            'https://edit.arabellaharcourtcooze.co.uk' +
            painting.Photo.data.attributes.url
          "
          :caption="painting.Name"
          :dimensions="painting.Dimensions"
          :sold="painting.Sold"
          :medium="painting.Medium"
        /> -->
        <!-- TODO: Mum wants the 'Oil on canvas' part to be a field in painting (Medium?) and displayed in the modal -->
      </div>
    </section>
  </div>
</template>

<script>
import PaintingModal from "@/components/PaintingModal.vue";
import { mapActions, mapGetters } from "vuex";
export default {
  name: "GalleryGrid",
  props: {
    slug: String,
  },
  methods: {
    imgClick(painting) {
      painting.show = true;
    },
    ...mapActions({
      fetchPaintings: "fetchPaintings",
    }),
  },
  created() {
    this.fetchPaintings(this.slug);
  },
  computed: {
    paintings() {
      return this.paintingsByGallerySlug(this.slug);
    },
    ...mapGetters({
      paintingsByGallerySlug: "paintingsByPageSlug",
    }),
  },
  components: {
    PaintingModal,
  },
};
</script>

<style scoped>
.grid {
  display: flex;
  /* margin:  */
  justify-content: center;
  flex-wrap: wrap;
  /* margin: 0 6%; */
  /* justify-content: space-between; */
}
.painting {
  display: flex;
  min-width: 0;
  max-width: 25%;
  margin: 5px;
  /* transition: 0.8s ease; */
  padding-bottom: 30px;
}
.painting p {
  /* position: absolute; */
  display: block;
  color: #33404e;
  /* transform: translate(0%, 120%); */
  margin-bottom: 0;
  align-self: flex-end;
}
.painting img {
  box-shadow: 5px 5px 10px 2px #838383;
  /* width: 450px; */
  max-height: 250px;
  max-width: 350px;
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* .modal img {
  max-width: 100%;
} */
</style>
