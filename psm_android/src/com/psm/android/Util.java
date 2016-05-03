package com.psm.android;

import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;

import org.json.JSONObject;

import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.TextView;

import com.facebook.android.Facebook;
import com.psm.android.fs.Foursquare;
import com.psm.util.Insider;

public class Util {

	private final static boolean ENABLE_DEBUG = true;
	public final static String FB_APPID = "0000000000";			//TODO: Change to your own FB_APPID
	public final static String YAHOO_APPID = "0000000";      	//TODO: Use Your own Yahoo_appid
	public final static String MAP_KEY = "00000000000"; 		//TODO: Map_key
	
	public static SessionManager mSession = new SessionManager();
	public static Facebook mFacebook;
	public static Foursquare mFoursquare;
	public static Insider mInsider;
	public static String mfacebookId;
	public static String minsiderId;
	public static String mfoursquareId;
	
	public static Bundle getQueryParams(String url)
	{
		Bundle bundle = new Bundle();
		if(url.contains("[?]") != true)
			return bundle;
		
		String splt[] = url.split("[&]");
		for(String string : splt)
		{
			String q[] = string.split("[=]", 2);
			bundle.putString(URLDecoder.decode(q[0]), URLDecoder.decode(q[1]));
		}
		
		return bundle;
		
	}
	
	public static void removeUnderlines(Spannable p_Text) {
	    URLSpan[] spans = p_Text.getSpans(0, p_Text.length(), URLSpan.class);

	    for(URLSpan span:spans) {
	        int start = p_Text.getSpanStart(span);
	        int end = p_Text.getSpanEnd(span);
	        p_Text.removeSpan(span);
	        span = new URLSpanNoUnderline(span.getURL());
	        p_Text.setSpan(span, start, end, 0);
	    }
	 }
	
	public static class URLSpanNoUnderline extends URLSpan {
	    public URLSpanNoUnderline(String p_Url) {
	        super(p_Url);
	    }

	    public void updateDrawState(TextPaint p_DrawState) {
	        super.updateDrawState(p_DrawState);
	        p_DrawState.setUnderlineText(false);
	    }
	}

	public static void clickify(TextView view, final String clickableText,  final ClickSpan.OnClickListener listener) {
        CharSequence text = view.getText();
        String string = text.toString();
        ClickSpan span = new ClickSpan(listener);

        int start = string.indexOf(clickableText);
        int end = start + clickableText.length();
        if (start == -1) return;

        if (text instanceof Spannable) {
            ((Spannable)text).setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else {
            SpannableString s = SpannableString.valueOf(text);
            s.setSpan(span, start, end, Spanned.SPAN_MARK_MARK);
            view.setText(s);
        }

        MovementMethod m = view.getMovementMethod();
        if ((m == null) || !(m instanceof LinkMovementMethod)) {
            view.setMovementMethod(LinkMovementMethod.getInstance());
        }
    }
	
	public static void clickify(TextView view, final int offset, final int length, final ClickSpan.OnClickListener listener) {
        CharSequence text = view.getText();
        String string = text.toString();
        ClickSpan span = new ClickSpan(listener);

        int start = offset;
        int end = start + length;
        if (start == -1) return;

        if (text instanceof Spannable) {
            ((Spannable)text).setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else {
            SpannableString s = SpannableString.valueOf(text);
            s.setSpan(span, start, end, Spanned.SPAN_MARK_MARK);
            view.setText(s);
        }

        MovementMethod m = view.getMovementMethod();
        if ((m == null) || !(m instanceof LinkMovementMethod)) {
            view.setMovementMethod(LinkMovementMethod.getInstance());
        }
    }
	public static class ClickSpan extends ClickableSpan {

	    private OnClickListener mListener;

	    public ClickSpan(OnClickListener listener) {
	        mListener = listener;
	    }

	    @Override
	    public void onClick(View widget) {
	       if (mListener != null) mListener.onClick();
	    }

	    public interface OnClickListener {
	        void onClick();
	    }

	    @Override
	    public void updateDrawState(TextPaint ds) {
	        ds.setColor(0xff3333ff);
	    }
	}

	public static String formatFSTime(long time)
	{
		Date date = new Date(time);
		
		SimpleDateFormat format = new SimpleDateFormat("MMM' 'd', 'yyyy");
		return format.format(date);
	}
	
	public static String formatSmsTime(long time)
	{
		return formatSmsTime(time, false);
	}
	public static String formatSmsTime(long time, boolean includeTime)
	{
		Date now = new Date();
		Date then = new Date(time);
		SimpleDateFormat otherformat = new SimpleDateFormat("EE', 'MMM' 'd', 'yyyy");
		//SimpleDateFormat otherformatwithtime = new SimpleDateFormat("EE', 'MMM' 'd', 'yyyy' 'h':'mm' 'aa");
		SimpleDateFormat timeformat = new SimpleDateFormat("h':'mm' 'aa");
		GregorianCalendar nowcal = new GregorianCalendar(now.getYear(), 
				now.getMonth(), now.getDay(), now.getHours(), now.getMinutes());
		
		GregorianCalendar thencal = new GregorianCalendar(then.getYear(), 
				then.getMonth(), then.getDay(), then.getHours(), then.getMinutes());
		
		int nowyear = nowcal.get(GregorianCalendar.YEAR);
		int thenyear = thencal.get(GregorianCalendar.YEAR);
		
		int nowday = nowcal.get(GregorianCalendar.DAY_OF_YEAR);
		int thenday = thencal.get(GregorianCalendar.DAY_OF_YEAR);
		
		if(nowyear == thenyear)
		{
			//Same year 
			if(nowday == thenday)
			{
				if(includeTime)
				{
					return timeformat.format(then);
				}
				else
					return "Today";
			}
			else
			{
				if(Math.abs(nowday-thenday) == 1)
				{
					if(includeTime)
					{
						return "Yesterday, " + timeformat.format(then);
					}
					else
						return "Yesterday";
				}
			}
		}
		
		if(includeTime)
			return otherformat.format(then) + ", " + timeformat.format(then);
		else
			return otherformat.format(then);
		
	}
	
	public static class JsonNameComparator implements Comparator<JSONObject>{

		public int compare(JSONObject lhs, JSONObject rhs) {
			try {
				String name1 = lhs.getString("name").toLowerCase();
				String name2 = rhs.getString("name").toLowerCase();
				return ((name1 == name2) ? 0 : ((name1.compareTo(name2) > 0) ? 1 : -1 ));
			}catch(Exception ex) {}
			return 0;
		}
		
	}
	
	
	
	
	
	public static void log(Object logStr)
	{
		if(ENABLE_DEBUG)
			System.out.println(String.valueOf(logStr));
	}
}
