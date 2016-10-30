package net.mbonnin.arcanetracker;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.regex.Pattern;

import timber.log.Timber;

/**
 * Created by martin on 10/21/16.
 */
public class CardsAdapter extends RecyclerView.Adapter{
    private int mClassIndex;
    private ArrayList<Card> mCardList = new ArrayList<>();
    private Listener mListener;
    private String mSearchQuery;
    private int mCost = -1;

    public void setCost(int cost) {
        mCost = cost;
        filter();
    }

    static class MyImageView extends ImageView {
        public MyImageView(Context context) {
            super(context);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int width = MeasureSpec.getSize(widthMeasureSpec);
            int height = width * 465 / 307;

            setMeasuredDimension(width, height);
        }
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public interface Listener {
        void onClick(Card card);
    }
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ImageView imageView = new MyImageView(parent.getContext());

        RecyclerView.LayoutParams layoutParams = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        int m = Utils.dpToPx(10);
        layoutParams.topMargin = layoutParams.leftMargin = layoutParams.rightMargin = layoutParams.bottomMargin = m;
        imageView.setLayoutParams(layoutParams);

        RecyclerView.ViewHolder holder = new RecyclerView.ViewHolder(imageView) {};

        imageView.setOnClickListener(v -> {
            int position = holder.getAdapterPosition();
            if (position < mCardList.size()) {
                // not really sure how this could happen...
                mListener.onClick(mCardList.get(position));
            }
        });
        return holder;
    }

    public void setClass(int classIndex) {
        mClassIndex = classIndex;

        filter();
    }

    public void setSearchQuery(String searchQuery) {
        if (searchQuery.equals("")) {
            mSearchQuery = null;
        } else {
            mSearchQuery = searchQuery.toLowerCase();
        }
        filter();

    }
    private void filter() {
        mCardList.clear();
        ArrayList<Card> allCards = ArcaneTrackerApplication.getCards();

        String playerClass = Card.classIndexToPlayerClass(mClassIndex);
        for (Card card: allCards) {
            if (card.collectible == null || !card.collectible) {
                continue;
            }

            if (card.cost == null) {
                continue;
            }

            if (mCost != -1) {
                if (mCost == 7 && card.cost < 7) {
                    continue;
                } else if (card.cost != mCost) {
                    continue;
                }
            }

            /*if (card.type == null
                    || (!card.type.equals("SPELL") && !card.type.equals("MINION") && !card.type.equals("WEAPON"))) {
                continue;
            }*/
            if (card.playerClass != null && playerClass == null
                    || (card.playerClass == null && playerClass != null)
                    || (playerClass != null && !playerClass.equals(card.playerClass))) {
                continue;
            }

            if (mSearchQuery != null) {
                boolean found = false;
                if (card.text != null && card.text.toLowerCase().indexOf(mSearchQuery) != -1) {
                    found = true;
                }

                if (!found && card.name != null && card.name.toLowerCase().indexOf(mSearchQuery) != -1) {
                    found = true;
                }

                if (!found && card.race != null && card.race.toLowerCase().indexOf(mSearchQuery) != -1) {
                    found = true;
                }

                if (!found) {
                    continue;
                }
            }

            mCardList.add(card);
        }

        Collections.sort(mCardList, (a, b) -> a.cost - b.cost);

        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ImageView imageView = (ImageView)holder.itemView;
        String baseUrl = "http://vps208291.ovh.net/cards/enus/";
        //String baseUrl = "http://wow.zamimg.com/images/hearthstone/cards/enus/original/";
        Card card = mCardList.get(position);
        String url = baseUrl + card.id + ".png";

        int placeHolderRes;
        if (card.rarity.equals(Card.RARITY_LEGENDARY)) {
            placeHolderRes = R.raw.placeholder_legendary;
        } else if (card.type.equals(Card.TYPE_SPELL)) {
            placeHolderRes = R.raw.placeholder_spell;
        } else {
            placeHolderRes = R.raw.placeholder_minion;
        }
        Timber.d("fetching " + url);
        Picasso.with(imageView.getContext())
                .load(url)
                .placeholder(placeHolderRes)
                .into(imageView);
    }

    @Override
    public int getItemCount() {
        return mCardList.size();
    }
}
