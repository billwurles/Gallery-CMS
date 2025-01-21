<script>
export default {
  name: "TheTitle", //TODO: figure out a better default title
  props: ["title"],
  watch: {
    title: {
      immediate: true,
      handler() {
        document.title = this.title;
      },
    },
  },
};
</script>
