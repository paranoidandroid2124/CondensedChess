type InfiniteScrollModule = {
  initModule?: (selector?: string) => void;
};

export default async function initModule(_opts?: unknown) {
  if (!document.querySelector('.infinite-scroll')) return;

  const infiniteScroll = await site.asset.loadEsm<InfiniteScrollModule>('bits.infiniteScroll');
  infiniteScroll.initModule?.('.infinite-scroll');
}
