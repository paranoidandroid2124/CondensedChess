export default async function initModule() {
  if (!document.querySelector('.infinite-scroll')) return;
  await site.asset.loadEsm('bits.infiniteScroll', { init: '.infinite-scroll' });
}
